### Steps
##### 1. Launch spark streaming app
    
    ~/spark-1.4.0-bin-hadoop2.6/bin/spark-submit --class com.compal.drama.draft.StreamAnalyzer  --master spark://spark-1:7077  --deploy-mode cluster /home/winston/draft/SparkStreamingAnalyzerAssembly-0.1.0.jar
    Note: if there is no cluster shared storage, 
        (1) SparkStreamingAnalyzerAssembly-0.1.0.jar might be pre-deployed at every spark worker node, 
        (2) create checkpoint folder (say, /home/winston/draft/spark/checkpoint/) at every spark worker node for experimental purpose

    
##### 2. Terminate spark streaming app

    ~/spark-1.4.0-bin-hadoop2.6/bin/spark-submit --kill <SubmissionID>  --master spark://draft2:6066  --deploy-mode cluster 
