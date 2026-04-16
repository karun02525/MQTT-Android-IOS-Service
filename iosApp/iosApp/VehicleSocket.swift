import Foundation

class VehicleSocket {

    var webSocketTask: URLSessionWebSocketTask?

    func connect() {
        let url = URL(string: "ws://YOUR_IP:8080/vehicle")!
        webSocketTask = URLSession.shared.webSocketTask(with: url)
        webSocketTask?.resume()

        receive()
    }

    func receive() {
        webSocketTask?.receive { result in
            switch result {
            case .success(let message):
                switch message {
                case .string(let text):
                    print("Received: \(text)")
                default:
                    break
                }
                self.receive()
            case .failure(let error):
                print("Error: \(error)")
            }
        }
    }
}