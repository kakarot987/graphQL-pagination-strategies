GraphQL Pagination Demo
=======================

Quick demo application that demonstrates two pagination strategies:
- Cursor-based (activity feed)
- Offset-based (products catalog, admin users)

Run the app
-----------
From PowerShell in the project root:

```powershell
.\gradlew.bat bootRun
```

Open GraphiQL: http://localhost:8080/graphiql

Demo sequence (what `demo.ps1` does)
-----------------------------------
1) Fetch the first 5 activities (cursor-based)
2) Create a new activity via `createActivity` mutation
3) Fetch the first 5 activities again to show the new activity at the top (cursor stability)

Example GraphQL queries
-----------------------
Fetch activity feed (first 5):

```graphql
query($first:Int){
  activityFeed(first:$first){
    edges{ cursor node{ id username activityType timestamp description } }
    pageInfo{ hasNextPage endCursor }
    feedMeta { unreadCount lastViewedTimestamp totalActivities }
  }
}
```

Create a new activity (mutation):

```graphql
mutation($userId:ID!,$username:String!,$activityType:ActivityType!,$description:String!,$targetId:String,$targetName:String,$metadata:String){
  createActivity(
    userId:$userId,
    username:$username,
    activityType:$activityType,
    description:$description,
    targetId:$targetId,
    targetName:$targetName,
    metadata:$metadata
  ){
    id username activityType timestamp description
  }
}
```

PowerShell demo script
----------------------
Run `.	ools\demo.ps1` (or `.
esources\demo.ps1` depending on placement) while the server is running. The script is also included in the repository as `demo.ps1`.

Notes & next steps
------------------
- Consider adding `before/last` support for bidirectional cursor pagination.
- Add keyset (seek) pagination for `products` to compare performance with offset.
- Add integration tests demonstrating offset inconsistency vs cursor stability.

Run with Docker
---------------
You can run the application with a single command using Docker Compose. The repository includes a multi-stage `Dockerfile` that builds the application inside a Gradle builder image and produces a runtime image.

Build and start (foreground):

```powershell
docker compose up --build
```

Build and start (detached):

```powershell
docker compose up --build -d
```

After the container starts the app is available at http://localhost:8080 and GraphiQL at http://localhost:8080/graphiql


