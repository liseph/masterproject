# masterproject

Format of input is:
```java -jar app.jar [alg_id] [test_id] [n_docs] [n_iterations] [n_topics] [path_in] [path_out]```
- `alg_id` = 0 for GeoLPTA, 1 for TopicPeriodica and 2 for PSTA+.
- `test_id` = 0 for simple run, 1 for varying the number of topics, 2 for varying the number of documents and 3 for timing reading the input.
- `n_docs` = number of documents to read from the input file
- `n_iterations` = number of iterations per setting to time the algorithm
- `n_topics` = number of topics, ignored if `test_id=1`.
- `path_in` = path to input file
- `path_out` = output file name
