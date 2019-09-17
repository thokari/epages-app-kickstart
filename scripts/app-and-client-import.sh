EPAGESJ_PRIVATE_API=${1:-localhost}

curl -v -X PUT -H "Content-Type: application/json" http://$EPAGESJ_PRIVATE_API:8088/rs/oauth2/import/clients --data @import-client-payload.json
curl -v -X PUT -H "Content-Type: application/json" http://$EPAGESJ_PRIVATE_API:8088/rs/appstore/import/apps --data @import-app-payload.json
