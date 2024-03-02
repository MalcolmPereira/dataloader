#! /usr/bin/env sh

# Create a new container with the Azure SQL Edge image, for local testing
docker run -e 'ACCEPT_EULA=1' -e 'MSSQL_SA_PASSWORD=highVolumeDataLoader2024*' -p 1433:1433 --name highvolumedb -d mcr.microsoft.com/azure-sql-edge

