```markdown
# Project Title: Multi-Threaded Web Crawler

## Description
This project implements a **multi-threaded web crawler** written in Java, designed to efficiently explore and extract data from websites. The crawler uses **Jsoup** for parsing HTML content and **ExecutorService** for managing concurrent threads. It recursively crawls a website starting from a specified URL, extracting and following links up to a defined maximum depth. 

The crawler ensures that only valid and unvisited links are processed, preventing redundant requests and efficiently managing resources. Additionally, it provides functionality for persisting the state of the crawl, allowing the process to resume from where it left off.

---

## Key Features
- **Multi-threading**:  
  By using ExecutorService, the crawler processes multiple URLs concurrently, making it scalable for larger websites and reducing the time spent on crawling.

- **HTML Parsing**:  
  The crawler employs Jsoup to extract and parse links from web pages, enabling easy navigation and extraction of relevant URLs.

- **Depth Control**:  
  The crawler respects a configurable maximum crawl depth to prevent over-crawling and avoid wasting resources on unnecessary links.

- **State Persistence**:  
  The list of visited URLs is saved to a file (`crawler_state.txt`), ensuring that the crawler can resume from the last visited URL if interrupted, improving fault tolerance.

- **Queue Management**:  
  The crawler uses a `BlockingQueue` to manage URLs efficiently, ensuring thread-safe access to URLs by multiple concurrent threads.

---

## Input
1. **Starting URL**:  
   The URL where the crawler begins its exploration (e.g., `https://www.linkedin.com/feed/`).

2. **Number of Threads**:  
   The number of concurrent threads the crawler should use for processing (1-10).

3. **Maximum Crawl Depth**:  
   The maximum depth the crawler will follow links recursively. This limits the scope of crawling to avoid excessive resource consumption.

---

## Output
1. **Crawler Output**:  
   On the console, the crawler prints the title of each crawled page along with any extracted links.

2. **State File**:  
   A file named `crawler_state.txt` stores all the visited URLs. This file allows the crawler to resume from where it left off in future runs.
```
