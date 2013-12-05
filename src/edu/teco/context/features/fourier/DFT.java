/*******************************************************************************
 * Copyright 2013 Karlsruhe Institute of Technology. This Work has been partially supported by the EIT ICT Labs funded research project Towards a Mobile Cloud (activity CLD 12206).
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package edu.teco.context.features.fourier;

public class DFT {

	// http://nayuki.eigenstate.org/page/how-to-implement-the-discrete-fourier-transform
	static void dft(double[] inreal, double[] inimag, double[] outreal, double[] outimag) {
		int n = inreal.length;
		for (int k = 0; k < n; k++) { // For each output element
			double sumreal = 0;
			double sumimag = 0;
			for (int t = 0; t < n; t++) { // For each input element
				sumreal += inreal[t] * Math.cos(2 * Math.PI * t * k / n) + inimag[t]
						* Math.sin(2 * Math.PI * t * k / n);
				sumimag += -inreal[t] * Math.sin(2 * Math.PI * t * k / n) + inimag[t]
						* Math.cos(2 * Math.PI * t * k / n);
			}
			outreal[k] = sumreal;
			outimag[k] = sumimag;
		}
	}

}
