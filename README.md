# UDP Notifier

![App Screenshot](<link_for_your_image_here>)

## About

UDP Notifier is a simple Android application that allows you to send and receive UDP messages over a network. It's a handy utility for testing UDP communication, debugging network applications, or creating simple notification systems.

## Features

*   **Listen for UDP Messages**: Start a listening service on any specified port.
*   **Send UDP Messages**: Send custom messages to any target IP address and port.
*   **Configurable Ports**: Easily change both the listening port and the target port for sending.
*   **Real-time Statistics**: Keep track of the number of received messages and the timestamp of the last one.
*   **IP Address Display**: Automatically detects and displays the device's local IP address.
*   **Persistent Configuration**: The app remembers your last used listening port, target IP, and target port.
*   **Service Status**: A clear indicator shows whether the listening service is active or stopped.
*   **Background Service**: The UDP listener runs as a foreground service, ensuring it remains active even when the app is in the background.

## How to Use

### Listening for Messages

1.  **Set the Listening Port**: In the "Listener Service Status" card, enter the port number you want to listen on in the "Listening port" field.
2.  **Start the Service**: Tap the "▶ START SERVICE" button. The status indicator will turn green, and the app will start listening for incoming UDP messages on the specified port.
3.  **View Statistics**: The "Received Messages Statistics" card will update automatically, showing the total count of received messages and the time the last one arrived.
4.  **Stop the Service**: To stop listening, tap the "⏹ STOP SERVICE" button.

### Sending Messages

1.  **Set the Target IP**: In the "Send UDP Message" card, enter the destination IP address in the "Target IP" field.
2.  **Set the Target Port**: Enter the destination port number in the "Target port" field.
3.  **Write Your Message**: Type the message you want to send in the "Message" field.
4.  **Send**: Tap the "🚀 SEND MESSAGE" button to send the UDP packet. You will see a confirmation toast message.
