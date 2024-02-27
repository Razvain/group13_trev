import matplotlib.pyplot as plt
import csv

def read_csv(file_path):
    timestamps = []
    error_codes = []
    with open(file_path, 'r') as file:
        reader = csv.reader(file)
        for row in reader:
            timestamps.append(float(row[0].replace(',', '.')))
            error_codes.append(float(row[1].replace(',', '.')))
    return timestamps, error_codes

random_fuzzer = 'path_random_fuzzer'
hill_climber = 'path_hill_climber'

timestamps1, error_codes1 = read_csv(random_fuzzer)
timestamps2, error_codes2 = read_csv(hill_climber)

cumulative_error_codes1 = [len(error_codes1[:i+1]) for i in range(len(error_codes1))]
cumulative_error_codes2 = [len(error_codes2[:i+1]) for i in range(len(error_codes2))]
fig = plt.figure()
plt.plot(timestamps1, cumulative_error_codes1, label='Random Fuzzer')
plt.plot(timestamps2, cumulative_error_codes2, label='Hill Climber')

plt.title('Cumulative Convergence Graph for Problem 17', fontweight = 'bold')
plt.xlabel('Timestamp (s)', fontweight = 'bold')
plt.ylabel('Number of Unique Error Codes', fontweight = 'bold')
plt.legend()
plt.grid(True)
# plt.show()

fig.savefig("prob17_plot.png", dpi=250)