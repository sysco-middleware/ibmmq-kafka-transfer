//package no.sysco.middleware.ibm.jms.kafka.rest;
//
//import com.linecorp.armeria.common.HttpRequest;
//import com.linecorp.armeria.common.HttpResponse;
//import com.linecorp.armeria.common.HttpStatus;
//import com.linecorp.armeria.server.Server;
//import com.linecorp.armeria.server.ServerBuilder;
//import java.util.concurrent.CompletableFuture;
//
//public class Application {
//  public static void main(String[] args) {
//    Server server =
//        new ServerBuilder()
//            .http(8085)
//            .service(
//                "/events",
//                (ctx, req) -> {
//                  // todo: produce msg to ibm_mq
//                  return HttpResponse.of(HttpStatus.OK);
//                })
//            .build();
//
//    CompletableFuture<Void> future = server.start();
//    // Wait until the server is ready.
//    future.join();
//  }
//}
