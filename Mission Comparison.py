import os

datafileList = []
missionList = []

activeMissions = []
failedMissions = []
abortedMissions = []
offeredMissions = []
declinedMissions = []
completedMissions = []

# Filter a string to only what's between double quotes
def filterToQuotes(s):
    q1 = s.find('"')
    q2 = s.find('"', q1 + 1)
    return s[q1+1:q2]

# Create a list of all data files
def listDatafiles(directory):
    global datafileList
    for filename in os.listdir(directory):
        f = os.path.join(directory, filename)
        if os.path.isfile(f):
            if f.endswith('.txt'):
                datafileList.append(f)
        else:
            listDatafiles(f)

# Find all the mission nodes in a datafile
def listMissionsInFile(f):
    global missionList
    file = open(f)
    for line in file:
        if line.startswith('mission'):
            missionName = filterToQuotes(line)
            missionList.append(missionName)

# Parse save file looking for missions of all kinds
def listMissionsByStatus(f):
    global activeMissions
    global failedMissions
    global abortedMissions
    global offeredMissions
    global declinedMissions
    global completedMissions
    
    file = open(f)
    search = False
    for line in file:
        if line == 'conditions\n' and not search:
            search = True
        elif not line.startswith('\t') and search:
            search = False
        if search:
            condition = filterToQuotes(line)
            if condition.endswith(': active'):
                activeMissions.append(condition[0:-8])
            elif condition.endswith(': failed'):
                failedMissions.append(condition[0:-8])
            elif condition.endswith(': aborted'):
                abortedMissions.append(condition[0:-9])
            elif condition.endswith(': offered'):
                offeredMissions.append(condition[0:-9])
            elif condition.endswith(': declined'):
                declinedMissions.append(condition[0:-10])
            elif condition.endswith(': done'):
                completedMissions.append(condition[0:-5])

# Check if a value is not in a list
def notIn(a, l):
    if a in l:
        return False
    return True

# Run the script
dataDir = input('Path to datafile directory: ')
listDatafiles(dataDir)
for file in datafileList:
    listMissionsInFile(file)
print('Data file crawling complete!')
savefile = input('Path to save file: ')
listMissionsByStatus(savefile)
print('Save file crawling complete!')

# Print the final report
print('\nThere were %s missions in the files loaded (missionList).' % len(missionList))
print('\nYou have completed %s of them (completedMissions).' % len(completedMissions))
print('You have %s currently active (activeMissions).' % len(activeMissions))
print('You have failed %s of them (failedMissions).' % len(failedMissions))
print('You have aborted %s of them (abortedMissions).' % len(abortedMissions))
print('You have been offered %s of them (offeredMissions).' % len(offeredMissions))
print('You have declined %s of them (declinedMissions).' % len(declinedMissions))

print('\nThe following missions are still incomplete:')
incomplete = list(filter(lambda a: notIn(a, completedMissions), missionList))
print(incomplete)
