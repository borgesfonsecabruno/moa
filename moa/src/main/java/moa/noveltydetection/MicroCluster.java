/*
 *    ClustreamKernel.java
 *    Copyright (C) 2010 RWTH Aachen University, Germany
 *    @author Jansen (moa@cs.rwth-aachen.de)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 *    
 */
package moa.noveltydetection;

import moa.cluster.CFCluster;
import moa.clusterers.clustream.ClustreamKernel;

import com.yahoo.labs.samoa.instances.Instance;

public class MicroCluster extends ClustreamKernel {
	private static final long serialVersionUID = 1L;
	private final static double EPSILON = 0.00005;
	
	private String category;
    private double classId;
    private Long time = (long) 0;
    
    public MicroCluster( Instance instance, int dimensions, long timestamp , double t, int m) {
        super(instance, dimensions, timestamp, t, m);
        this.radiusFactor = 2.0;

    }

    public MicroCluster( ClustreamKernel cluster, double t, int m ) {
        super(cluster,t, m);
        this.radiusFactor = 2.0;
    }

    @Override
    public double getRadius() {
        //trivial cluster
    	if(N == 1) return 0;

       return this.getDeviationMinas()*2.0;
    }
    
    private double[] getVarianceVectorMinas() {
        double[] res = new double[this.LS.length];
        for (int i = 0; i < this.LS.length; i++) {
            double ls = this.LS[i];
            double ss = this.SS[i];

            double lsDivN = ls / this.getWeight();
            double lsDivNSquared = lsDivN * lsDivN;
            double ssDivN = ss / this.getWeight();
            res[i] = ssDivN - lsDivNSquared;

            // Due to numerical errors, small negative values can occur.
            // We correct this by settings them to almost zero.
            if (res[i] <= 0.0) {
                if (res[i] > -EPSILON) {
                    res[i] = MIN_VARIANCE;
                }
            }
            else{
                
            }
        }
        return res;
    }
    
    private double getDeviationMinas(){
        double[] variance = getVarianceVectorMinas();
        
        double sumOfDeviation = 0.0;
        for (int i = 0; i < variance.length; i++) {
            sumOfDeviation += variance[i];
        }
        return Math.sqrt(sumOfDeviation);
    }

    @Override
    public CFCluster getCF(){
        return this;
    }

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Long getTime() {
		return time;
	}

	public void setTime(Long time) {
		this.time = time;
	}

	public double getClassId() {
		return classId;
	}

	public void setClassId(double classId) {
		this.classId = classId;
	}
	
	public double getRadiusFactor() {
		return this.radiusFactor;
	}
}
