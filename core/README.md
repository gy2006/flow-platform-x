## Status List

#### Status for job

* PENDING: Initial status

* ENQUEUE: Been put to job queue

* RUNNING: Agent received the job and start to execute

* SUCCESS: Job been executed 

* FAILURE: Job been executed but failure

* STOPPED: Job been stopped by user

* TIMEOUT: Job execution time been over the expiredAt

#### Status for step

* PENDING

* RUNNING

* SUCCESS

* EXCEPTION

* KILLED

* TIMEOUT

## Websocket API

Init connection: `{host}/ws`

- Example:
  ```javascript
  const socket = new SockJS('http://127.0.0.1/ws')
  stompClient = Stomp.over(socket);
  stompClient.connect({}, function (frame) {
    console.log('Connected: ' + frame);
  });
  ```

Cmd logging: `/topic/logs/{cmd id}`

- Example:
  ```javascript
  const path = '/topic/logs/' + step.id;
  stompClient.subscribe(path, function (data) {
    console.log(data.body + " =======");
  });
  ```