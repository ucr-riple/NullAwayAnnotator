import xml.etree.cElementTree as ET


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
