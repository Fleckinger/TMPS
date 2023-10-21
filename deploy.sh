#!/bin/bash
echo "Start deployment script."

start=$(date +"%s")
echo "Trying establish SSH connection..."
ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} -i key.txt -t -t -o StrictHostKeyChecking=no << 'ENDSSH'
echo "SSH connection established."

docker pull ${DOCKER_CONTAINER_NAME}:latest

CONTAINER_NAME=${DOCKER_CONTAINER_NAME}
if [ "$(docker ps -qa -f name=$CONTAINER_NAME)" ]; then
    if [ "$(docker ps -q -f name=$CONTAINER_NAME)" ]; then
        echo "Container is running -> stopping it..."
        docker stop $CONTAINER_NAME;
    fi
fi

echo "Run docker container..."

docker run --detach --name $CONTAINER_NAME -p 8082:8082 ${DOCKER_CONTAINER_NAME}:latest --SERVER_PORT=${APP_SERVER_PORT} --SPRING_DATASOURCE_URL=jdbc:postgresql://${DATABASE_URL} --SPRING_DATASOURCE_USERNAME=${DATABASE_USERNAME} --SPRING_DATASOURCE_PASSWORD=${DATABASE_PASSWORD} --TELEGRAM_BOT_NAME=${TELEGRAM_BOT_NAME} --TELEGRAM_BOT_ID=${TELEGRAM_BOT_ID} --TELEGRAM_BOT_TOKEN=${TELEGRAM_BOT_TOKEN} --POSTING_DELAY=60000

exit
ENDSSH

if [ $? -eq 0 ]; then
  exit 0
else
  exit 1
fi

end=$(date +"%s")

diff=$(($end - $start))

echo "Deployed in : ${diff}s"