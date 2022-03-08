import os
import sys
import json
import shutil
import time
import xmltodict


if not (len(sys.argv) in [2, 3]):
    raise ValueError(
        "Needs one argument to run: diagnose/apply/pre/loop/clean")

data = None
if int(len(sys.argv)) == 2:
    data = json.load(open('config.json'))
else:
    data = json.load(open(sys.argv[2]))

if 'REPO_ROOT_PATH' not in data:
    # By default, the path to the repo root (where the build command must be run) and the
    # path to the project source is the same. This works for most gradle projects.
    data['REPO_ROOT_PATH'] = data['PROJECT_PATH']

build_command = "cd {} && {} && cd {}".format(data['REPO_ROOT_PATH'],
                                              data['BUILD_COMMAND'],
                                              data['PROJECT_PATH'])
out_dir = "/tmp/NullAwayFix"
delimiter = "\t"
format_style = str(data['FORMAT']).lower()
format_style = "false" if format_style not in ["true", "false"] else format_style

EXPLORER_CONFIG = json.load(open('template.xml'))


def load_csv_to_dict(path):
    ans = []
    csvFile = open(path, 'r')
    lines = csvFile.readlines()
    keys = lines[0].strip().split(delimiter)
    for line in lines[1:]:
        item = {}
        infos = line.strip().split(delimiter)
        for i in range(0, len(keys)):
            item[keys[i]] = infos[i]
        ans.append(item)
    return ans


def make_explorer_config(config):
    with open('/tmp/NullAwayFix/explorer.config', 'w') as outfile:
        json.dump(config, outfile)


def delete(file):
    try:
        os.remove(file)
    except OSError:
        pass


def uprint(message):
    print(message, flush=True)
    sys.stdout.flush()


def clean(full=True):
    uprint("Cleaning...")
    delete(out_dir + "/diagnose_report.json")
    delete(out_dir + "/fixes.csv")
    delete(out_dir + "/diagnose.json")
    delete(out_dir + "/cleaned.json")
    delete(out_dir + "/init_methods.json")
    delete(out_dir + "/method_info.csv")
    delete(out_dir + "/errors.csv")
    if (full):
        delete(out_dir + "/reports.json")
        delete(out_dir + "/log.txt")
    uprint("Finished.")


def prepare():
    if not os.path.exists(out_dir):
        os.makedirs(out_dir)
    failed = {"fixes": []}
    with open(out_dir + "/failed.json", 'w') as outfile:
        json.dump(failed, outfile)


def pre():
    uprint("Started preprocessing task...")
    method_path = out_dir + "/method_info.csv"
    delete(method_path)
    delete(out_dir + "/init_methods.json")
    uprint("Building project...\n" + build_command)
    new_config = EXPLORER_CONFIG.copy()
    new_config['SUGGEST']['ACTIVE'] = True
    new_config['MAKE_METHOD_INHERITANCE_TREE'] = True
    new_config['MAKE_CALL_GRAPH'] = True
    new_config['MAKE_FIELD_GRAPH'] = True
    new_config['LOG_ERROR']['ACTIVE'] = True
    new_config['LOG_ERROR']['DEEP'] = True
    new_config['ANNOTATION']['NULLABLE'] = data['ANNOTATION']['NULLABLE']
    new_config['ANNOTATION']['NONNULL'] = data['ANNOTATION']['NONNULL']
    make_explorer_config(new_config)
    os.system(build_command + " > /dev/null 2>&1")
    uprint("Analyzing suggested fixes...")
    fixes = load_csv_to_dict(out_dir + "/fixes.csv")
    uprint("Detecting uninitialized class fields...")
    field_no_inits = [
        x for x in fixes
        if (x['reason'] == 'FIELD_NO_INIT' and x['location'] == 'CLASS_FIELD')
    ]
    uprint("Found " + str(len(field_no_inits)) + " fields.")
    uprint("Analyzing method infos...")
    methods = load_csv_to_dict(method_path)
    init_methods = {"fixes": []}
    uprint("Selecting appropriate method for each class field...")
    for field in field_no_inits:
        candidate_method = None
        max = 0
        for method in methods:
            if (method['class'] == field['class']):
                if (field['param'] in method['fields']
                        and len(method['fields']) > max):
                    candidate_method = method.copy()
                    max = len(method['fields'])
        if (candidate_method != None):
            del candidate_method['fields']
            candidate_method['location'] = "METHOD_RETURN"
            candidate_method['inject'] = True
            candidate_method['annotation'] = data['ANNOTATION']['INITIALIZE']
            candidate_method['param'] = ""
            candidate_method['reason'] = "Initializer"
            candidate_method['pkg'] = ""
            if candidate_method not in init_methods['fixes']:
                init_methods['fixes'].append(candidate_method)
    with open(out_dir + "/init_methods.json", 'w') as outfile:
        json.dump(init_methods, outfile)
    uprint("Passing to injector to annotate...")
    os.system("cd jars && java -jar core.jar apply {}/init_methods.json {}".format(out_dir, format_style))
    uprint("Finshed.")


def diagnose():
    new_config = EXPLORER_CONFIG.copy()
    new_config['SUGGEST']['ACTIVE'] = True
    new_config['LOG_ERROR']['ACTIVE'] = True
    new_config['LOG_ERROR']['DEEP'] = True
    new_config['ANNOTATION']['NULLABLE'] = data['ANNOTATION']['NULLABLE']
    new_config['ANNOTATION']['NONNULL'] = data['ANNOTATION']['NONNULL']
    make_explorer_config(new_config)
    build_command = '"cd ' + data['REPO_ROOT_PATH'] + " && " + data[
        'BUILD_COMMAND'] + '"'
    uprint("Detected build command: " + build_command)
    uprint("Starting AutoFixer...")
    command = "cd jars && java -jar core.jar diagnose {} {} {} {} {}".format(out_dir, build_command, str(data['DEPTH']),
                                                                             data['ANNOTATION']['NULLABLE'],
                                                                             format_style)
    print(command)
    os.system(command)
    uprint("Finsihed.")


def apply():
    delete(out_dir + "/cleaned.json")
    report_file = open(out_dir + "/diagnose_report.json")
    reports = json.load(report_file)
    cleaned = {}
    uprint("Selecting effective fixes...")
    cleaned['fixes'] = [fix for fix in reports['reports'] if fix['jump'] < 1]
    with open(out_dir + "/cleaned.json", 'w') as outfile:
        json.dump(cleaned, outfile)
    uprint("Applying fixes at location: " + out_dir + "/cleaned.json")
    os.system("cd jars && java -jar core.jar apply {}/cleaned.json {}".format(out_dir, format_style))


def loop():
    uprint("Executing loop command")
    delete(out_dir + "/log.txt")
    finished = False
    while not finished:
        finished = True
        diagnose()
        uprint("Diagnsoe task finished, applying effective fixes...")
        apply()
        uprint("Applied.")
        new_reports = json.load(open(out_dir + "/diagnose_report.json"))
        if len(new_reports['reports']) == 0:
            uprint("No changes, shutting down.")
            break
        old_reports = json.load(open(out_dir + "/reports.json"))
        for report in new_reports['reports']:
            if report not in old_reports['reports']:
                finished = False
                old_reports['reports'].append(report)
        with open(out_dir + "/reports.json", 'w') as outfile:
            json.dump(old_reports, outfile)
            outfile.close()
    clean(full=False)


command = sys.argv[1]
prepare()
if command == "pre":
    pre()
elif command == "diagnose":
    diagnose()
elif command == "apply":
    apply()
elif command == "loop":
    clean()
    pre()
    reports = open(out_dir + "/reports.json", "w")
    empty = {"reports": []}
    json.dump(empty, reports)
    reports.close()
    start = time.time()
    loop()
    end = time.time()
    print("Elapsed time in seconds: " + str(end - start))
elif command == "clean":
    clean()
    delete_folder = input("Delete " + out_dir + " directory too ? (y/n)\n")
    if delete_folder.lower() in ["yes", "y"]:
        try:
            shutil.rmtree(out_dir)
        except:
            uprint("Failed to remove directory: " + out_dir)
elif command == "reset":
    clean()
    try:
        shutil.rmtree(out_dir)
    except:
        uprint("Failed to remove directory: " + out_dir)

else:
    raise ValueError("Unknown command.")
