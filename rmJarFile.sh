#!/bin/bash
7z d target/aws-lambda-1.0-SNAPSHOT.jar org/bytedeco/javacpp/properties/android*
7z d target/aws-lambda-1.0-SNAPSHOT.jar aws-lambda-1.0-SNAPSHOT.jar org/bytedeco/javacpp/windows-x86_64
7z d target/aws-lambda-1.0-SNAPSHOT.jar aws-lambda-1.0-SNAPSHOT.jar org/bytedeco/javacpp/macosx-x86_64
