adb -e shell ps | grep "com.appenjoyment.lfnw" | head -1 | cut -d " " -f 5 | xargs adb -e shell kill
