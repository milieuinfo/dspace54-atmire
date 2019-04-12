#!/usr/bin/env python
# -*- coding: utf-8 -*-
#
# Provisions community/collection/groups/policies into DSpace
# using Java administrator classes. This is basically a Python wrapper
# around a Java utility.

import argparse
import xmltodict
import os
import subprocess
import errno
import sys


__version__ = '0.0.1'

response_encoding = "utf-8"


def make_parser():
    parser = argparse.ArgumentParser(description='Provisions community/collection/groups/policies into DSpace!')
    parser.add_argument('-v', '--version', action='version', version='%(prog)s ' + __version__)
    parser.add_argument('-f', '--file', help='import structure file', default='community-tree.xml')
    parser.add_argument('-b', '--dspace-bin-file', help='dspace binary file', default='./dspace/bin/dspace')
    parser.add_argument('-x', '--continue-on-exception', help='continue if import fails', action='store_true', default=False)
    return parser


def main():
    parser = make_parser()
    arguments = parser.parse_args()

    # importing the community.xml structure
    structure_builder = arguments.dspace_bin_file + ' structure-builder -f ' + arguments.file + \
                        ' -o community-output.xml -e dspace@milieuinfo.be'
    structure_builder_exit_code = os.system(structure_builder)

    # if importing went well we iterate over the successful communities (from the output xml file)
    # and add a group and policy for every community...
    if structure_builder_exit_code == 0:
        with open("community-output.xml", "r") as structure_file:
            import_structure = xmltodict.parse(structure_file.read())

        communities = import_structure['imported_structure']['community']
        if isinstance(communities, list) == 0:
            communities=list();
            communities.append(import_structure['imported_structure']['community'])


        for community in communities:
            print "Creating group for: " + community['name'] + " (" + community['@identifier'] + ")"
            community_id = community['@identifier']
            create_group = arguments.dspace_bin_file + ' create-group -n ' + community['name']
            try:
                create_group_output = subprocess.check_output(create_group.split(), shell=False)
                group_id = int(create_group_output.rsplit(":", 1)[1])

                print "Creating policy for group: " + community['name'] + " (" + community['@identifier'] + ")"
                create_resource_policy = arguments.dspace_bin_file + ' create-resource-policy -g ' + str(group_id) + ' -i ' + \
                                         community_id + ' -t 4 -a 11'
                create_resource_policy_output = subprocess.check_output(create_resource_policy.split(), shell=False)
                print create_resource_policy_output
            except subprocess.CalledProcessError:
                if arguments.continue_on_exception:
                    print "Something went wrong while importing communities from xml file! Failure will be ignored..."
                else:
                    print "Something went wrong while importing communities from xml file!"
                    sys.exit(errno.EBADEXEC)
    else:
        print "Something went wrong while importing communities from xml file!"
        sys.exit(errno.EBADEXEC)

if __name__ == '__main__':
    main()
