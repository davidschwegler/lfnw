adb shell run-as com.appenjoyment.lfnw ls -l databases
adb shell run-as com.appenjoyment.lfnw chmod 666 databases/Sessions.db
adb pull /data/data/com.appenjoyment.lfnw/databases/Sessions.db
adb shell run-as com.appenjoyment.lfnw chmod 660 databases/Sessions.db
