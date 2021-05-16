# masterproject

Format of input is:
```java -jar app.jar [alg_id] [test_id] [n_docs] [n_iterations] [path_in] [path_out]```
- `alg_id` = 0 for GeoLPTA, 1 for TopicPeriodica and 2 for PSTA+.
- `test_id` = 0 for simple run, 1 for varying the number of topics and 2 for varying the number of documents.
- `n_docs` = total number of documents in the input
- `n_iterations` = number of iterations per setting to time the algorithm
- `path_in` = path to input file
- `path_out` = output file name
