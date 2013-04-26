
# send the UPDATE_STARTED intent
adb -e shell am broadcast -a com.appenjoyment.lfnw.UpdateSessionsService.UPDATE_STARTED

# sleep 10 seconds
sleep 10

# send the UPDATE_COMPLETED intent
adb -e shell am broadcast -a com.appenjoyment.lfnw.UpdateSessionsService.UPDATE_COMPLETED

sleep 10

# update the list
adb -e shell am broadcast -a com.appenjoyment.lfnw.UpdateSessionsService.UPDATED_SESSIONS
