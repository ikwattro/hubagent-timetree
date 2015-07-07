GraphAware Neo4j TimeTree (Custom)
==================================

Custom Fork of the [GraphAware Neo4j TimeTree module](https://github.com/graphaware/neo4j-timetree)


### Modifications

#### Initialize

The rebuild of the timetree has been disabled by default. You can define in the configuration which event nodes labels need to be
fetched for the initialization run.

```
# Labels on which to run the initialize run separated by a comma.
com.graphaware.module.ID.initializedLabels=Event,Participation
```

#### DynamicRoot

You can define dynamic root based on a `label + property` combination and a `property value reference` on the event node.
For example, each user can be his own time tree, in that case, instead of issuing an http call for getting the user id to be
defined as timetree root, you'll be able benefit from autoAttach with only a `user_id` property on your event node containing the
user_id of your User node and a simple configuration line :

```
com.graphaware.module.ID.dynamicRoot=User:id:user_id
```

Here you define that your timetree root will have the label User with an id property and that your event node will have
a user_id property being the value for the MATCH of the TimeTree Root. Which will result in a

```
database.findNodes(DynamicLabel.label("User"), "id", created.getProperty("user_id"));
```

call.



License
-------

Copyright (c) 2014 GraphAware

GraphAware is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with this program.
If not, see <http://www.gnu.org/licenses/>.
