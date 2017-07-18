#!/bin/bash

set -e

docker build -t nikhilrayaprolu/yacygridmcp:$TRAVIS_COMMIT ./docker
docker login -u="$DOCKER_USERNAME" -p="$DOCKER_PASSWORD"
docker push nikhilrayaprolu/yacygridmcp
echo $GCLOUD_SERVICE | base64 --decode -i > ${HOME}/gcloud-service-key.json
gcloud auth activate-service-account --key-file ${HOME}/gcloud-service-key.json

gcloud --quiet config set project $PROJECT_NAME_STG
gcloud --quiet config set container/cluster $CLUSTER_NAME_STG
gcloud --quiet config set compute/zone ${CLOUDSDK_COMPUTE_ZONE}
gcloud --quiet container clusters get-credentials $CLUSTER_NAME_STG


kubectl config view
kubectl config current-context

kubectl set image deployment/${KUBE_DEPLOYMENT_NAME} ${KUBE_DEPLOYMENT_CONTAINER_NAME}=nikhilrayaprolu/yacygridmcp:$TRAVIS_COMMIT

# sleep 30
# npm run e2e_test