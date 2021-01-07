
import seaborn as sns
import pandas as pd
import matplotlib.pyplot as plt

"""
   datasize   keys_2   keys_128   keys_256   keys_512   keys_1024
0        10        1          2          3          4           5
1       100        1          2          3          4           5
2      1000        1          2          3          4           5
3     10000        1          2          3          4           5
4    100000        1          2          3          4           5
"""

def main():
	
	df_1_sv_pool_1 = pd.read_csv('results/1_sv_pool_1_thread.csv')
	df_1_sv_pool_2 = pd.read_csv('results/1_sv_pool_2_thread.csv')

	plt.show()

if __name__ == "__main__":
	main()
