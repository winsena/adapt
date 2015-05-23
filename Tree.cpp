/*
 * Tree.cpp
 *
 *  Created on: Mar 24, 2015
 *      Author: Tadeze
 */
#include "Tree.hpp"
using namespace std;
void Tree::iTree(vector<int> const &dIndex, int height, int maxheight, bool stopheight)
{
	this->depth = height; //Tree height
	// Set size of the node
	nodeSize = dIndex.size();
	//stop growing if condition
	if (dIndex.size() <= 1 || (stopheight && this->depth > maxheight))
	{
		//this->isLeaf = true;
		return;
	}
//*** Need modification
	//Initialize minmax array for holding max and min of an attributes
	vector <vector<double> > minmax;
	for (int j = 0; j < dt->ncol; j++)
	{
		vector<double> tmp;
		tmp.push_back(dt->data[0][j]);
		tmp.push_back(dt->data[0][j]);
		minmax.push_back(tmp); //initialize max and min to random value
	}



	try
	{
		for (int i = 0; i < dt->nrow-1; i++)
		{
			//vector<double> inst = data->data[i];
			for (int j = 0; j < dt->ncol; j++)
			{
				if (dt->data[i][j] < minmax[j].at(0))
					minmax[j].at(0) = dt->data[i][j];
				if (dt->data[i][j] > minmax.at(j).at(1))
					minmax[j].at(1) = dt->data[i][j];
			}

		}

		//use only valid attributes

		vector<int> attributes;
		for (int j = 0; j < dt->ncol; j++)
		{
			if (minmax[j][0] < minmax[j][1])
			{
				attributes.push_back(j);
			}
		}
		if (attributes.size() == 0)
			return;

		//Randomly pick an attribute and a split point
		int randx = randomI(0, attributes.size());
		this->splittingAtt = attributes[randx];
		this->splittingPoint = randomD(minmax[this->splittingAtt][0],
				minmax[this->splittingAtt][1]);
		vector <int> lnodeData;
		vector < int> rnodeData;

		//Split the node into two
		for (int j = 0; j < dt->nrow-1; j++)
		{
			if (dt->data[j][splittingAtt] > splittingPoint)
			{
				lnodeData.push_back(j);
			}
			else
			{
				rnodeData.push_back(j);
			}
		}

		/*Data dataL =
		{ data.ncols, (int) lnodeData.size(), lnodeData };
		*/
		leftChild = new Tree(); //&dataL,height+1,maxheight);
		leftChild->parent = this;
		leftChild->iTree(lnodeData, this->depth + 1, maxheight, stopheight);

		/*Data dataR =
		{ data.ncols, (int) rnodeData.size(), rnodeData };
		*/
		rightChild = new Tree(); //&dataR,height+1,maxheight);
		rightChild->parent = this;
		rightChild->iTree(rnodeData, this->depth + 1, maxheight, stopheight);
	}

	catch (const exception& er)
	{
		ffile << "Error in tree building..." << er.what() << "\n";
	}

}
//PathLength for an instance
/*
 * takes instance as vector of double
 */
double Tree::pathLength(double* inst)
{

 if (this->leftChild==NULL||this->rightChild==NULL)
	        { ///referenced as null for some input data .
	                return avgPL(this->nodeSize);
	        }


	if (inst[this->splittingAtt] > this->splittingPoint)
	{

		return this->leftChild->pathLength(inst) + 1.0;

	}
	else
	{
		return this->rightChild->pathLength(inst) + 1.0;
	}
}

