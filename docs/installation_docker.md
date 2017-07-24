# Installation of Yacy with Docker

Docker is a way to move applications including the operating system to servers.

## Get a Docker image

Each of the following sections depend on a docker image with a Yacy server inside.
This section shows you how to get such an image.
Some possibilities may be used in the sections but you can may be able to exchange them.

#### Pull a docker image
Pull a docker image from [dockerhub](https://hub.docker.com/r/nikhilrayaprolu/yacygridmcp/).

Type this command in your terminal:
        ```
        docker pull nikhilrayaprolu/yacygridmcp
        ```

#### Build a docker image on your local machine

1. Type the following commands in terminal
   ```
   sudo apt-get update && sudo apt-get upgrade -y
   sudo apt-get install docker.io
   sudo docker build https://github.com/yacy/yacy_grid_mcp.git
   ```

2. You will get a hash from last command. Copy that hash and do...
   ```
   sudo docker tag <IMAGE_HASH> <NAME_FOR_IMAGE>
   ```

#### Automated build on Cloud using GitHub repository

1. Signup for [Docker](https://cloud.docker.com/)
2. Go to Settings > Linked Accounts & Services.
3. Add your GitHub account and select an access level “Public and Private”, or “Limited Access”.
4. Go to your GitHub account. Settings > Applications.
5. Allow access for Docker Hub Registry.
6. Allow service hook for the repository in its settings > Webhooks & Services.
7. Select a source repository to build image in Docker Hub.
8. Enable rebuilding the Docker image when a commit is pushed to the GitHub repository.
9. Congratulations, you have completed the setup of automated image build.
