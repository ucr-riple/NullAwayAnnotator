import os

root = os.path.dirname(os.path.abspath(os.getcwd()))
os.system("cd {} && ./gradlew :core:fatJar".format(root))
os.system((
    "mv {} {}".format(os.path.join(root, "core", "build", "libs", "core.jar"), os.path.join(root, "runner", "jars", "core.jar"))))
