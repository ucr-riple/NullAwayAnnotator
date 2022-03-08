import os
import json
import sys
import shutil
import filecmp

PROJECT_PATH = "{}/tests".format(os.getcwd())
ALL_TESTS_PATH = "../tests/units/"

data = json.load(open('../tests/config.json'))
data['PROJECT_PATH'] = PROJECT_PATH
with open('../tests/config.json', 'w') as outfile:
    json.dump(data, outfile, indent=4)

def print_status(msg: str, status):
    GREEN = "\033[1;32;40m"
    RED = "\033[1;31;40m"
    NORMAL = '\033[0m'
    color = GREEN if status else RED
    print("{}{}{}".format(color, msg, NORMAL))


def compare_dirs(dir1: str, dir2: str):
    dirs_cmp = filecmp.dircmp(dir1, dir2)
    if len(dirs_cmp.left_only)>0 or len(dirs_cmp.right_only)>0 or \
        len(dirs_cmp.funny_files)>0:
        return False, []
    (_, mismatch, errors) =  filecmp.cmpfiles(
        dir1, dir2, dirs_cmp.common_files, shallow=False)
    if len(mismatch)>0 or len(errors)>0:
        return False, mismatch
    for common_dir in dirs_cmp.common_dirs:
        new_dir1 = os.path.join(dir1, common_dir)
        new_dir2 = os.path.join(dir2, common_dir)
        status, dif = compare_dirs(new_dir1, new_dir2)
        if(not status):
            return False, dif
    return True, []

def show_tests():
    return [x for x in os.listdir(ALL_TESTS_PATH) if x != "build" and os.path.isdir(ALL_TESTS_PATH + x)]

def test(name: str):
    if(not name in show_tests()):
        print("No test found with name: {}".format(name))
        return
    TEST_DIR = ALL_TESTS_PATH + name + "/{}"
    try:
        shutil.rmtree(TEST_DIR.format("out/"))
    except OSError as e:
        print("Error in test{}".format(name))
    os.system("cp -R {} {}".format(TEST_DIR.format("src/"), TEST_DIR.format("tmp/")))

    data = json.load(open('../tests/config.json'))
    data['PROJECT_PATH'] = PROJECT_PATH
    data['BUILD_COMMAND'] = "./gradlew :units:{}:build -x test".format(name)
    with open('../tests/config.json', 'w') as outfile:
        json.dump(data, outfile, indent=4)
    os.system("python3 run.py loop tests/config.json")
    os.renames(TEST_DIR.format("src/"), TEST_DIR.format("out/"))
    os.renames(TEST_DIR.format("tmp/"), TEST_DIR.format("src/"))
    os.system("cd {} && ./gradlew goJF".format(PROJECT_PATH))
    status, difs = compare_dirs(TEST_DIR.format("out/main/"), TEST_DIR.format("expected/main/"))
    if(status):
        print_status("{} - TEST WAS SUCCESSFUL".format(name), True)
    else:
        print_status("{} - TEST WAS UNSUCCESSFUL - {}".format(name, difs), False)

def test_all():
    print("Executing all tests")
    for name in show_tests():
        test(name)

COMMAND = sys.argv[1]
if(COMMAND == "tests"):
    print("All tests are:")
    for test in show_tests():
        print(test)
elif(COMMAND == "test"):
    if(len(sys.argv) == 3):
        test(sys.argv[2])
    else:
        test_all()

