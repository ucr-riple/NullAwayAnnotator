import xml.etree.cElementTree as ET


def write_dict_config_in_xml(config, path):
    root = ET.Element("serialization")

    # Suggest
    ET.SubElement(root, "suggest", activation=config['serialization']['suggest']['@activation'],
                  enclosing=config['serialization']['suggest']['@enclosing'])

    # Output path
    ET.SubElement(root, "output").text = config['serialization']['output']

    # Annotations
    annotations = ET.SubElement(root, "annotation")
    ET.SubElement(annotations, "nullable").text = config['serialization']['annotation']['nullable']
    ET.SubElement(annotations, "nonnull").text = config['serialization']['annotation']['nonnull']

    # Param Test
    ET.SubElement(root, "paramTest", activation=config['serialization']['paramTest']['@activation'],
                  index=str(config['serialization']['paramTest']['@index']))

    # Field Init Info
    ET.SubElement(root, "fieldInitInfo", activation=config['serialization']['fieldInitInfo']['@activation'])

    tree = ET.ElementTree(root)
    tree.write(path)
