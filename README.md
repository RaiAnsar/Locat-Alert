

# Geo Alarm

Geo Alarm is a location based reminder app. Geo Alarm allows users to set alarm/reminders all over the map and keeps track of them using the *Geofencing API*.
Users can use Geo Alarm to create *geofences*, and create alarms to ring on entry , or on exit from a location.


Geo Alarm uses the user's location to alert them when they reach their destination. This is especially useful for long journeys.
Factors such as traffic, the route you choose etc. can affect the total duration of your journey. This is why conventional time based alarms are not useful while traveling.


*Note: Geo Alarm may not work underground (e.g. while travelling by metro underground)*

## Build
Using Firebase as a backend for authentication and storage.
To build this app, you must set up your own Firebase project, then:

 - Copy your *google-services.json* file to the *app* folder.
 - For cloud functions, copy your *admin service account JSON file* to functions folder and rename it *adminServiceAccountKey.json*

Furethermore, your Firebase project must have **Blaze** plan, since this project uses Cloud Functions.