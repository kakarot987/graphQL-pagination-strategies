param([string]$Uri = 'http://localhost:8080/graphql')
$ErrorActionPreference = 'Stop'
function Gql([string]$query, [hashtable]$variables = @{}) {
  $payload = @{query=$query; variables=$variables} | ConvertTo-Json -Depth 10 -Compress
  $response = Invoke-RestMethod -Uri $Uri -Method POST -ContentType 'application/json' -Body $payload
  if ($response.errors) { throw ($response.errors | ConvertTo-Json -Depth 8) }
  return $response.data
}
function Ids($nodes) { return @($nodes | ForEach-Object { [string]$_.id }) }
$cursorQ='query($after:String){activityFeed(first:5,after:$after){nodes{id} pageInfo{endCursor}}}'
$offsetQ='query($page:Int!){activityFeedOffset(page:$page,size:5){nodes{id}}}'
$mutation='mutation{createActivity(userId:"1",username:"demo",activityType:VIEW,description:"Inserted between pages"){id}}'
$first=Gql $cursorQ
$end=$first.activityFeed.pageInfo.endCursor
$oldIds=Ids $first.activityFeed.nodes
$before=Ids (Gql $cursorQ @{after=$end}).activityFeed.nodes
$offsetBefore=Ids (Gql $offsetQ @{page=1}).activityFeedOffset.nodes
$created=Gql $mutation
$after=Ids (Gql $cursorQ @{after=$end}).activityFeed.nodes
$offsetAfter=Ids (Gql $offsetQ @{page=1}).activityFeedOffset.nodes
if (($before -join ',') -ne ($after -join ',')) { throw 'Cursor page changed after insert' }
if (@($after | Where-Object { $oldIds -contains $_ }).Count -gt 0) { throw 'Cursor duplicated an ID' }
if (@($offsetAfter | Where-Object { $oldIds -contains $_ }).Count -eq 0) { throw 'Expected offset drift after new-head insert' }
Write-Host "PASS: cursor stable, offset drift demonstrated; inserted $($created.createActivity.id)" -ForegroundColor Green
Write-Host "Offset before: $($offsetBefore -join ',')"
Write-Host "Offset after:  $($offsetAfter -join ',')"
