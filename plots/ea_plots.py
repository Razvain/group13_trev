import matplotlib.pyplot as plt
import csv

def read_csv(file_path):
    timestamps = []
    fitness_scores = []
    with open(file_path, 'r') as file:
        reader = csv.reader(file)
        previous_fitness = None
        for row in reader:
            time = float(row[0].replace(',', '.'))
            fitness = float(row[1].replace(',', '.'))
            if fitness != previous_fitness:  # Remove duplicates
                timestamps.append(time)
                fitness_scores.append(fitness)
                previous_fitness = fitness
    return timestamps, fitness_scores

ea_files = ['/path/to/ea_data2.csv', '/path/to/ea_data3.csv']

timestamps_0_9, fitness_scores_0_9 = read_csv('./convergence_graphs_0_9/convergence_graph_prob_15.csv')
timestamps_0_95, fitness_scores_0_95 = read_csv('./convergence_graphs_0_95/convergence_graph_prob_15.csv')
timestamps_0_98, fitness_scores_0_98 = read_csv('./convergence_graphs_0_98/convergence_graph_prob_15.csv')
timestamps_cross_0_5, fitness_scores_cross_0_5 = read_csv('./convergence_graphs_crossover_5/convergence_graph_prob_15.csv')
timestamps_mp_1, fitness_scores_mp_1 = read_csv('./convergence_graphs_mutation_rate_1/convergence_graph_prob_15.csv')

fig = plt.figure()

plt.plot(timestamps_0_9, fitness_scores_0_9, label=f'MR=0.9, CR=0.2, MP=3')
plt.plot(timestamps_0_95, fitness_scores_0_95, label=f'MR=0.95, CR=0.2, MP=3')
plt.plot(timestamps_0_98, fitness_scores_0_98, label=f'MR=0.98, CR=0.2, MP=3')
plt.plot(timestamps_cross_0_5, fitness_scores_cross_0_5, label=f'MR=0.9, CR=0.5, MP=2')
plt.plot(timestamps_mp_1, fitness_scores_mp_1, label=f'MR=0.9, CR=0.2, MP=1')

plt.title('Convergence Graph for Problem 15', fontweight='bold')
plt.xlabel('Time (s)', fontweight='bold')
plt.ylabel('Fitness Score', fontweight='bold')
plt.legend()
plt.grid(True)

fig.savefig("convergence_plot_prob_15.png", dpi=250)
plt.show()