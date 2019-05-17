import argparse
import json
import numpy as np
from collections import Counter

# day_boundaries = {'1': {'min': , 'max': },
#                   '2': {'min': , 'max': },
#                   '3': {'min': , 'max': },
#                   '4': {'min': , 'max': },
#                   '5': {'min': , 'max': },
#                   '6': {'min': , 'max': },
#                   '7': {'min': , 'max': },
#                   '8': {'min': , 'max': },
#                   '9': {'min': , 'max': }}

# days in engagement: [1,2,3,4,7,8,9,10,11]
engagement_days = [0,1,2,3,6,7,8,9,10]
begin_e5 = 1557226800

begin_of_day = lambda x: 86400 * x + begin_e5
end_of_day = lambda x: 86400 * x + begin_e5 + 32400
def in_e5_day(d, t):
    return (t < end_of_day(d)) and (t > begin_of_day(d))

day_from_timestamp = lambda t: int(np.floor((t - begin_e5)/86400)) # t in seconds

def in_e5_range(t):
    return any([in_e5_day(d,t) for d in engagement_days])


def test(filepath):
    alarms = []
    with open(filepath, 'r') as f:
        lines = f.readlines()
        for line in lines:
            alarms.append(line)
    return lines

def read_splunk_log(filepath):
    alarms = {'raw': [], 'aggregatedAlarm': [], 'prioritizedAlarm': []}
    with open(filepath, 'r') as f:
        lines = f.readlines()
        # runID = lines[0]
        for line in lines[5:]:
            if not line.strip(): #(len(line.strip()) != 0):
                try:
                    alarm_line = json.loads(line)
                    alarm_category = alarm_line['metadata']['alarm_category']
                    alarms[alarm_category].append(alarm_line)
                except:
                    print(":( \n {}")
                    #return None
    return alarms

def datatime_distribution(alarm_list):
    dataTimes = [] # in seconds
    for line in alarm_list:
        dataTimes.append(int(min(line['dataTimestamps'])/1e9))
    return dataTimes

def alarms_from_host(alarm_list, hostname):
    return [a for a in alarm_list if hostname in a['alarm']['hostName']]

def partition_by_day(alarm_list):
    partition = dict([(d,[]) for d in engagement_days])
    filtered = [x for x in alarm_list if in_e5_range(int(min(x['alarm']['dataTimestamps'])/1e9)) ]
    for alarm in filtered:
        d = day_from_timestamp(int(min(alarm['alarm']['dataTimestamps'])/1e9))
        partition[d].append(alarm)

    return partition

def filter_by_proc_names(alarm_list, bad_proc_names):
    return [a for a in alarm_list if not any([p in a['alarm']['processName'] for p in bad_proc_names])]

def get_proc_names(alarm_list):
    return [a['alarm']['processName'] for a in alarm_list]

def proc_names_from_file(filename):
    procs = []
    with open(filename, 'r') as f:
        for line in procs:
            line = line.strip()
            if line:
                procs.append(line)
    return procs

if __name__ == "__main__":

    PROCS_TO_IGNORE = ["salt"]

    parser = argparse.ArgumentParser(description='E5 splunk alarm parser.')
    parser.add_argument('--ta1',
                        choices=['clearscope',
                                 'cadets',
                                 'fivedirections',
                                 'trace',
                                 'theia',
                                 'marple'])
    parser.add_argument('--splunklog', help="Provide full path to splunk alarm file log \
    which is likely located in the ppm_e5 directory.")
    parser.add_argument('--stats', action='store_true', default=False,
                        help="Write a file of data timestamps of 'prioritizedAlarm's for easy plotting.")
    parser.add_argument('--names', action='store_true', default=False,
                        help="Write a file of 'prioritizedAlarm' process names and their frequencies.")
    parser.add_argument('--details', action='store_true', default=False)
    parser.add_argument('--badprocfile', default="",
                        help="Provide full path to newline-separated list of process names to filter out.")

    args = parser.parse_args()

    ta1 = args.ta1
    splunk_file_path = args.splunklog

    all_procs_to_ignore = proc_names_from_file(args.badprocfile) + PROCS_TO_IGNORE \
        if args.badprocfile else PROCS_TO_IGNORE


    alarms = alarms_from_host(read_splunk_log(splunk_file_path)['prioritizedAlarm'], ta1)
    filtered_alarms = filter_by_proc_names(alarms, all_procs_to_ignore)

    alarms_by_day = partition_by_day(filtered_alarms)
    host_name = filtered_alarms[0]['alarm']['hostName']

    if args.stats:
        data_times = [t for t in datatime_distribution(a) for a in alarms_by_day.values()]
        with open(host_name + "_time_series.csv",'w') as f:
            f.write("host_name,time\n")
            for line in data_times:
                f.write(host_name + "," + str(line)+"\n")


    if args.names:
        process_counter = Counter([p for p in get_proc_names(ps) for ps in alarms_by_day.values()])
        process_counter = sorted(process_counter.items(), key = lambda x: -x[-1])

        with open(host_name + "_process_names.txt", 'w') as f:
            f.write("process_name,count\n")
            for (p,c) in process_counter:
                f.write(p + "," + str(c) + "\n")


    if args.details:
        proc_indent = "\t"
        detail_indent = proc_indent * 2
        with open(ta1 + "_process_details.txt", 'w') as f:
            for (day, alarm_list) in alarms_by_day:
                f.write("May {}, 2019 suspicious processes.\n".format(day + 7))
                for alarm in alarm_list:
                    f.write(proc_indent + alarm['alarm']['processName'])
                    details = alarm['alarm']['details'].split("\n")
                    for line in details:
                        if not "(none)" in line:
                            f.write(detail_indent + line)
