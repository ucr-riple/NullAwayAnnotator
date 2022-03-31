# MIT License
#
# Copyright (c) 2020 Nima Karimipour
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.

import os
import sys
import json
import shutil
import time
import xmltodict
import tools
from tools import delete
from tools import uprint
from os.path import join

if not (len(sys.argv) in [2, 3]):
    raise ValueError("Needs one argument to run: diagnose/apply/pre/loop/clean")

if int(len(sys.argv)) == 2:
    data = json.load(open('config.json'))
else:
    data = json.load(open(sys.argv[2]))

if 'REPO_ROOT_PATH' not in data:
    # By default, the path to the repo root (where the build command must be run) and the
    # path to the project source is the same. This works for most gradle projects.
    data['REPO_ROOT_PATH'] = data['PROJECT_PATH']

build_command = "cd {} && {} && cd {}".format(data['REPO_ROOT_PATH'], data['BUILD_COMMAND'], data['PROJECT_PATH'])
out_dir = data['OUTPUT_DIR']
nullaway_config_path = data['NULLAWAY_CONFIG_PATH']
css_config_path = data['CSS_CONFIG_PATH']
format_style = str(data['FORMAT']).lower()
format_style = "false" if format_style not in ["true", "false"] else format_style


def clean(full=True):
    uprint("Cleaning...")
    delete(join(out_dir, "diagnose_report.json"))
    delete(join(out_dir, "fixes.tsv"))
    delete(join(out_dir, "diagnose.json"))
    delete(join(out_dir, "cleaned.json"))
    delete(join(out_dir, "init_methods.json"))
    delete(join(out_dir, "method_info.tsv"))
    delete(join(out_dir, "errors.tsv"))
    if full:
        delete(join(out_dir, "reports.json"))
        delete(join(out_dir, "log.txt"))


def prepare():
    if not os.path.exists(out_dir):
        os.makedirs(out_dir)
    failed = {"fixes": []}
    with open(join(out_dir, "failed.json"), 'w') as outfile:
        json.dump(failed, outfile)


def build_project(init_active="true"):
    uprint("Building project...")
    new_config = xmltodict.parse(open('template.xml').read()).copy()
    new_config['serialization']['annotation']['nullable'] = data['ANNOTATION']['NULLABLE']
    new_config['serialization']['annotation']['nonnull'] = data['ANNOTATION']['NONNULL']
    new_config['serialization']['fieldInitInfo']['@active'] = init_active
    tools.write_nullaway_config_in_xml(new_config, nullaway_config_path)
    os.system(build_command + " > /dev/null 2>&1")


def apply_fixes_at(path):
    tools.run_jar("apply", path, format_style)


def preprocess():
    tools.write_css_config_in_xml(True, out_dir, css_config_path)
    uprint("Started preprocessing task...")
    method_path = join(out_dir, "field_init.tsv")
    delete(method_path)
    delete(join(out_dir, "init_methods.json"))
    build_project()
    if data['DEPTH'] < 0 or (not data['OPTIMIZED']):
        return
    fixes = tools.load_tsv_to_dict(out_dir + "/fixes.tsv")
    uprint("Detecting uninitialized class fields...")
    field_no_inits = [x for x in fixes if (x['reason'] == 'FIELD_NO_INIT' and x['location'] == 'FIELD')]
    uprint("Detecting initializers...")
    raw_methods = tools.load_tsv_to_dict(method_path)
    init_methods = {"fixes": []}
    methods = []
    for method in raw_methods:
        seen = None
        for discovered in methods:
            if discovered['method'] == method['method'] and discovered['class'] == method['method']:
                seen = discovered
                break
        if seen is None:
            method['fields'] = []
            seen = method
            methods.append(seen)
        seen['fields'].append(method['field'])
    for field in field_no_inits:
        candidate_method = None
        max_size = 0
        for method in methods:
            if method['class'] == field['class']:
                if field['param'] in method['fields'] and len(method['fields']) > max_size:
                    candidate_method = method.copy()
                    max_size = len(method['fields'])
        if candidate_method is not None:
            del candidate_method['fields']
            candidate_method['location'] = "METHOD"
            candidate_method['inject'] = True
            candidate_method['annotation'] = data['ANNOTATION']['INITIALIZER']
            candidate_method['param'] = ""
            candidate_method['reason'] = "Initializer"
            if candidate_method not in init_methods['fixes']:
                init_methods['fixes'].append(candidate_method)
    uprint("Annotating as {}".format(data['ANNOTATION']['INITIALIZER']))
    init_methods_path = join(out_dir, "init_methods.json")
    with open(init_methods_path, 'w') as outfile:
        json.dump(init_methods, outfile)
    apply_fixes_at(init_methods_path)


def explore():
    tools.write_css_config_in_xml(False, out_dir, css_config_path)
    uprint("Starting Exploration Phase...")
    tools.run_jar("explore", nullaway_config_path, "'{}'".format(build_command), data['DEPTH'], data['ANNOTATION']['NULLABLE'], format_style, data['OPTIMIZED'])


def apply_effective_fixes():
    delete(join(out_dir, "cleaned.json"))
    report_file = open(join(out_dir, "diagnose_report.json"))
    reports = json.load(report_file)
    cleaned = {}
    uprint("Selecting effective fixes...")
    cleaned['fixes'] = [fix for fix in reports['reports'] if fix['effect'] < 1]
    with open(join(out_dir, "cleaned.json"), 'w') as outfile:
        json.dump(cleaned, outfile)
    apply_fixes_at(join(out_dir, "cleaned.json"))


def run():
    uprint("Executing run command")
    delete(join(out_dir, "log.txt"))
    finished = False
    while not finished:
        finished = True
        explore()
        uprint("Explore task finished, applying effective fixes...")
        apply_effective_fixes()
        new_reports = json.load(open(join(out_dir, "diagnose_report.json")))
        if len(new_reports['reports']) == 0:
            uprint("No changes, shutting down.")
            break
        old_reports = json.load(open(join(out_dir, "reports.json")))
        for report in new_reports['reports']:
            if report not in old_reports['reports']:
                finished = False
                old_reports['reports'].append(report)
        with open(join(out_dir, "reports.json"), 'w') as outfile:
            json.dump(old_reports, outfile)
            outfile.close()
    clean(full=False)


command = sys.argv[1]
prepare()
if command == "preprocess":
    preprocess()
elif command == "explore":
    explore()
elif command == "apply":
    apply_effective_fixes()
elif command == "run":
    clean()
    preprocess()
    reports = open(join(out_dir, "reports.json"), "w")
    empty = {"reports": []}
    json.dump(empty, reports)
    reports.close()
    start = time.time()
    run()
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
else:
    raise ValueError("Unknown command. should be one of ['preprocess', 'explore', 'apply', 'run', 'clean']")
