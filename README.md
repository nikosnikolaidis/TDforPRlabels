# TDforPRlabels

A tool for analyzing github pull requests before and after their merging, with the use of SonarQube.

This tool was developed as part of a research namely *"Exploring the Effect of Different Maintenance Activities on the Accumulation of TD Principal"*.

## Usage

In order to use this tool, you will the following:
- A working SonarQube instance
- A GitHub Fine-grained personal access tokens

You will need to manually add the GitHub key in the GitHubAPIauthorization variable from the Main. And as for the SonarQube url, you will need to provide it in the sonar-scanner.properties