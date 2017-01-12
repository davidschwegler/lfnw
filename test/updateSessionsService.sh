
# force check
adb -e shell am startservice -n "com.appenjoyment.lfnw/.UpdateSessionsService" --es "startKind" "forced"

# only check if interval expired
adb -e shell am startservice -n "com.appenjoyment.lfnw/.UpdateSessionsService" --es "startKind" "interval"

