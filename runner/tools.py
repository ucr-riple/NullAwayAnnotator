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
import xml.etree.cElementTree as ET
import sys


def uprint(message):
    print(message, flush=True)
    sys.stdout.flush()


def delete(file):
    try:
        os.remove(file)
    except OSError:
        pass


def load_tsv_to_dict(path):
    ans = []
    csv_file = open(path, 'r')
    lines = csv_file.readlines()
    keys = lines[0].strip().split("\t")
    for line in lines[1:]:
        item = {}
        infos = line.strip().split("\t")
        for i in range(0, len(keys)):
            item[keys[i]] = infos[i]
        ans.append(item)
    return ans


def write_dict_config_in_xml(config, path):
    root = ET.Element("serialization")

    # Suggest
    ET.SubElement(root, "suggest", active=config['serialization']['suggest']['@active'],
                  enclosing=config['serialization']['suggest']['@enclosing'])

    # Output path
    ET.SubElement(root, "path").text = config['serialization']['path']

    # Annotations
    annotations = ET.SubElement(root, "annotation")
    ET.SubElement(annotations, "nullable").text = config['serialization']['annotation']['nullable']
    ET.SubElement(annotations, "nonnull").text = config['serialization']['annotation']['nonnull']

    # Param Test
    ET.SubElement(root, "paramTest", active=config['serialization']['paramTest']['@active'],
                  index=str(config['serialization']['paramTest']['@index']))

    # Field Init Info
    ET.SubElement(root, "fieldInitInfo", active=config['serialization']['fieldInitInfo']['@active'])

    tree = ET.ElementTree(root)
    tree.write(path, xml_declaration=True, encoding='utf-8')


def run_jar(command, *args):
    arguments = ""
    for arg in args:
        arguments += str(arg) + " "
    os.system("cd jars && java -jar core.jar {} {}".format(command, arguments.strip()))

