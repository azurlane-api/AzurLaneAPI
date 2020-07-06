module.exports = {
    apps: [
        {
            name: "AzurLane",
            script: "java",
            args: "-jar azurlane-api-2.0.0.jar",
            instances: 1,
            autorestart: true,
            watch: false,
            exec_mode: "fork",
            env: {
                NODE_ENV: "development",
                FORCE_COLOR: 1
            },
            env_production: {
                NODE_ENV: "production"
            }
        }
    ]
};
