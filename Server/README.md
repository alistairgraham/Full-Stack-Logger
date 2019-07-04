# Server

## Jacoco
`mvn jacoco:report`
Open *target/site/jacoco/index.html*

BlackBox tests: I did not produce Jacoco for the blackbox tests which tested /logs and /stats.

WhiteBox tests: WhiteBox tests the /logs server. The class that holds my doGet and doPost methods is called LogService.
- doGet: 100% instruction and 92% branch coverage.
    - The one condition that is not fully explored is if(a==null||b==null). 1/4 of the branches are missed because I
never tested for when both conditions are false. This is not necessary.
- doPost: 94% instruction and 76% branch coverage.
    - The condition if(request==null||response==null) misses two possible branches because I never tested for if they were never null. I did not find this possible
to do the way I was testing the class however I left the condition there as a failsafe. The body of the condition, *return*, is subsequently never run.
    - I explore 5/8 branches of the condition (logsJSON == null || logsJSON.isEmpty() || req.getContentType()==null || !req.getContentType().startsWith(ContentType.APPLICATION_JSON.getMimeType())).
I don't explore more because that's just using different combinations that don't really need to be explored in this case because the conditional statement only uses OR. They are not reliant on eachother.
    - If the json from the request cannot be parsed into a LogEvent I have a catch clause that will be triggered. This never triggers although I do pass in an empty
JSON array and an imparsable String in my tests. The conditions before it return from the method earlier if that's the case. It is still there because I was not able to
pass in null through the way I was testing so it is a failsafe.
    - The other two conditions that aren't fully explored are similar to point #3. All combinations of OR conditionals don't need to be explored.

## JDepend
`mvn site`
Open *target/site/jdepend-report.html*

Distance to main path: 0

The package is coincident with the main sequence. There is no abstraction in the package.

## Spotbugs
`mvn spotbugs:spotbugs`
`mvn spotbugs:gui`
 - A bug is found where I am inefficiently iterating through HashMap.getKeys() and then using HashMap.get(key) with the keys. It is more efficient to use HashMap.entrySet().
 This raised a good point however the keys are actually the values in this case and the values are a count of how many times a key has been seen. I was unsure if entrySet could
work with this so I did not try at the time. Spotbugs highlighting this has definitely made me think about my choice however and I will try using entrySet in the future.