#! /usr/bin/env python3

# Copyright 2016, Palo Alto Research Center.
# Developed with sponsorship of DARPA.
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
# The software is provided "AS IS", without warranty of any kind, express or
# implied, including but not limited to the warranties of merchantability,
# fitness for a particular purpose and noninfringement. In no event shall the
# authors or copyright holders be liable for any claim, damages or other
# liability, whether in an action of contract, tort or otherwise, arising from,
# out of or in connection with the software or the use or other dealings in
# the software.
#

from .detector import Detector
import re

__author__ = 'John.Hanley@parc.com'


class MarkerDetector(Detector):
    '''Identifies "events of interest" located between begin / end markers.'''

    def __init__(self, gremlin):
        self.gremlin = gremlin
        # This regex will need to be adjusted for THEIA. ("/tmp/...")
        self._marker_re = re.compile(
            r'^file:///tmp/adapt/tc-marker-(\d{3})-(begin|end)\.txt$')

    def name_of_input_property(self):
        return 'url'

    def find_activities(self, seg_id, seg_props):
        activities = []
        for prop in seg_props:
            # if seg_id == 1486952:
            #     print(seg_id, prop)
            try:
                url = prop[self.name_of_input_property()]
            except KeyError:
                continue  # Url not present.
            m = self._marker_re.search(url)
            if m:  # if finds_feature
                name_of_output_classification = 'marker_events_' + m.group(2)
                ident = prop['ident']
                print(prop['sequence'], m.group(1), name_of_output_classification, url)
                activities.append((ident, name_of_output_classification))
        return activities
