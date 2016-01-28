/*
 * Forest.cpp
 *
 *  Created on: Aug 12, 2015
 *      Author: tadeze
 */

#include "Forest.hpp"

double Forest::getdepth(double* inst, Tree* tree) {
	return tree->pathLength(inst);
}

/*
 * Accepts single point (row) and return Anomaly Score
 */
double Forest::instanceScore(double *inst) {

	double avgPathLength = mean(pathLength(inst));
	double scores = pow(2, -avgPathLength / avgPL(this->nsample));
	return scores;

// unnormalized raw depth score
//	double scores = pow(2, -avgPathLength / avgPL(this->nsample));
//	return avgPathLength;//scores;

}

void Forest::writeScoreDatabase(doubleframe *dtTestNorm, doubleframe *dtTestAnom, char fName[]){
	std::ofstream out(fName);
	out << "algotype,groundtruth,instID,treeID,dlim,score\n";
	for(int i = 0; i < dtTestNorm->nrow; ++i){
		for(int t = 0; t < this->ntree; ++t){
			std::vector<double> allDepthScores = this->trees[t]->getDepthEstforAllNodes(dtTestNorm->data[i]);
			for(unsigned int dlim = 0; dlim < allDepthScores.size(); ++dlim)
				out << "I" << ","
					<< "N" << ","
					<< i+1 << ","
					<< t+1 << ","
					<< dlim + 1 << ","
					<< allDepthScores[dlim] << std::endl;
		}
	}
	for(int i = 0; i < dtTestAnom->nrow; ++i){
		for(int t = 0; t < this->ntree; ++t){
			std::vector<double> allDepthScores = this->trees[t]->getDepthEstforAllNodes(dtTestAnom->data[i]);
			for(unsigned int dlim = 0; dlim < allDepthScores.size(); ++dlim)
				out << "I" << ","
					<< "A" << ","
					<< dtTestNorm->nrow+i+1 << ","
					<< t+1 << ","
					<< dlim + 1 << ","
					<< allDepthScores[dlim] << std::endl;
		}
	}
	for(int i = 0; i < dtTestNorm->nrow; ++i){
		for(int t = 0; t < this->ntree; ++t){
			std::vector<double> allDepthScores = this->trees[t]->getPatternScores(dtTestNorm->data[i]);
			for(unsigned int dlim = 0; dlim < allDepthScores.size(); ++dlim)
				out << "P" << ","
					<< "N" << ","
					<< i+1 << ","
					<< t+1 << ","
					<< dlim + 1 << ","
					<< allDepthScores[dlim] << std::endl;
		}
	}
	for(int i = 0; i < dtTestAnom->nrow; ++i){
		for(int t = 0; t < this->ntree; ++t){
			std::vector<double> allDepthScores = this->trees[t]->getPatternScores(dtTestAnom->data[i]);
			for(unsigned int dlim = 0; dlim < allDepthScores.size(); ++dlim)
				out << "P" << ","
					<< "A" << ","
					<< dtTestNorm->nrow+i+1 << ","
					<< t+1 << ","
					<< dlim + 1 << ","
					<< allDepthScores[dlim] << std::endl;
		}
	}
	out.close();
}

std::vector<double> Forest::getScore(doubleframe *df, int type){
	std::vector<double> pscores[df->nrow];
	for(int i = 0; i < df->nrow; ++i){
		for(int t = 0; t < this->ntree; ++t){
			std::vector<double> psi = this->trees[t]->getPatternScores(df->data[i]);
			for(unsigned int j = 0; j < psi.size(); ++j)
				pscores[i].push_back(psi[j]);
		}
	}
	std::vector<double> res;
	double min, max, sum;
	for(int i = 0; i < df->nrow; ++i){
		min = max = sum = pscores[i][0];
		for(unsigned int j = 0; j < pscores[i].size(); ++j){
			if(min > pscores[i][j]) min = pscores[i][j];
			if(max < pscores[i][j]) max = pscores[i][j];
			sum += pscores[i][j];
		}
		if(type == 1)// minimum
			res.push_back(min);
		else if(type == 2)// mean
			res.push_back(sum/pscores[i].size());
		else if(type == 3){// median
			std::sort(pscores[i].begin(), pscores[i].end());
			res.push_back(pscores[i][pscores[i].size()/2]);
		}else// maximum
			res.push_back(max);
	}
	return res;
}

/*
 * Score for  a set of dataframe in dataset
 */
std::vector<double> Forest::AnomalyScore(doubleframe* df) {
	std::vector<double> scores;
	//iterate through all points
	for (int inst = 0; inst < df->nrow; inst++) {
		scores.push_back(instanceScore(df->data[inst]));
	}
	return scores;
}
/*
 * Return instance depth in all trees
 */

std::vector<double> Forest::pathLength(double *inst) {
	std::vector<double> depth;
	for (std::vector<Tree*>::iterator it = this->trees.begin();
			it != trees.end(); ++it) {

		depth.push_back((*it)->pathLength(inst));

	}
	// pnt++;   //for logging purpose
	return depth;
}

/* PathLength for all points
 */
std::vector<std::vector<double> > Forest::pathLength(doubleframe* data) {
	std::vector<std::vector<double> > depths;
	for (int r = 0; r < data->nrow; r++)
		depths.push_back(pathLength(data->data[r]));
	return depths;
}
/*
 * Anderson_Darling test from the pathlength
 */
/*
 vector<double> IsolationForest::ADtest(const vector<vector<double> > &pathlength,bool weighttotail)
 {

 //Will fix later
 return ADdistance(pathlength,weighttotail);
 }*/
/* Compute the feature importance of a point
 * input: *inst data instance
 * output: feature importance
 * status: Incomplete!!
 */
std::vector<double> Forest::importance(double *inst) {
	//Need to be re-implemented
	//Incorrect code
	std::vector<double> depth;
	for (std::vector<Tree*>::iterator it = this->trees.begin();
			it != trees.end(); ++it) {

		depth.push_back(ceil((*it)->pathLength(inst)));

	}
	return depth;
}

void Forest::getSample(std::vector<int> &sampleIndex, int nsample,
		bool rsample, int nrow) {
	if(nsample > nrow || nsample <= 0)
		nsample = nrow;
	int *rndIdx = new int[nrow];
	for(int i = 0; i < nrow; ++i)
		rndIdx[i] = i;
	for(int i = 0; i < nsample; ++i){
		int r = rand() % nrow;
		int t = rndIdx[i];
		rndIdx[i] = rndIdx[r];
		rndIdx[r] = t;
	}
	for(int i = 0; i < nsample; ++i){
		sampleIndex.push_back(rndIdx[i]);
	}
	delete []rndIdx;
}

void Forest::printStat(std::ofstream &out){
	for(int i = 0; i < this->ntree; ++i){
		this->trees[i]->printDepthAndNodeSize(out);
		break;
//		out << "#####################################" << std::endl;
	}
}

void Forest::printPatternFreq(const doubleframe *df, int n, std::ofstream &out){
	out << "inst_id, tree_id, depth, nodes, volume, log_nodes_d_vol" << std::endl;
	for(int iid = 0; iid < n; iid++){
		for(int tid = 0; tid < this->ntree; tid++){
			this->trees[tid]->printPatternFreq(df->data[iid], tid, iid, out);
		}
	}
}
