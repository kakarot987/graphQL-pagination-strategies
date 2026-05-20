param()

$uri = 'http://localhost:8080/graphql'

function Invoke-Gql($query, $variables) {
    $body = @{ query = $query; variables = $variables } | ConvertTo-Json -Depth 10
    return Invoke-RestMethod -Uri $uri -Method Post -ContentType 'application/json' -Body $body
}

Write-Host "Fetching first 5 activities..."
$query = 'query($first:Int){ activityFeed(first:$first){ edges{ cursor node{ id username activityType timestamp description } } pageInfo{ hasNextPage endCursor } feedMeta{ unreadCount lastViewedTimestamp totalActivities } } }'
$resp = Invoke-Gql $query @{ first = 5 }
Write-Host "Result:`n" ($resp | ConvertTo-Json -Depth 6)

Write-Host "Creating a new activity via mutation..."
$mutation = 'mutation($userId:ID!,$username:String!,$activityType:ActivityType!,$description:String!,$targetId:String,$targetName:String,$metadata:String){ createActivity(userId:$userId, username:$username, activityType:$activityType, description:$description, targetId:$targetId, targetName:$targetName, metadata:$metadata){ id username activityType timestamp description } }'
$vars = @{ userId = 1; username = 'demo_user'; activityType = 'PURCHASE'; description = 'Demo purchase via script'; targetId = 'order-999'; targetName = 'Demo Product'; metadata = '{"source":"ps1"}' }
$mutResp = Invoke-Gql $mutation $vars
Write-Host "Mutation result:`n" ($mutResp | ConvertTo-Json -Depth 6)

Write-Host "Fetching first 5 activities again..."
$resp2 = Invoke-Gql $query @{ first = 5 }
Write-Host "Result after insert:`n" ($resp2 | ConvertTo-Json -Depth 6)

Write-Host "Demo complete. Use GraphiQL at http://localhost:8080/graphiql for interactive exploration."

