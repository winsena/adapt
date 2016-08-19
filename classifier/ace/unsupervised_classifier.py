# -*- coding: utf-8 -*-

from sklearn import cluster
import numpy
import pprint

class UnsupervisedClassifier(object):
    def __init__(self, provenanceGraph, featureExtractor):
        self.provenanceGraph = provenanceGraph
        self.featureExtractor = featureExtractor

    def classifyNew(self):
        segmentIds = []
        features = []

        for segmentId, G in self.provenanceGraph.getUnclassifiedSegments():
            segmentIds.append(segmentId)
            features.append(self.featureExtractor.run(G))
        
        X = numpy.array(features)
        if len(X) == 0:
            return ()
            
        # Create a clustering estimator
#        estimator = cluster.AffinityPropagation() # doesn't work very well for some reason
        estimator = cluster.MeanShift()
        y = estimator.fit_predict(X)

        result = zip(segmentIds, y)

        return result
