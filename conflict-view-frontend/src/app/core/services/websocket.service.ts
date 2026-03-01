import { Injectable, OnDestroy } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Subject, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class WebSocketService implements OnDestroy {
  private client!: Client;
  private connected = false;

  connect(): void {
    if (this.connected) return;
    this.client = new Client({
      webSocketFactory: () => new SockJS(environment.wsUrl),
      reconnectDelay: 5000,
      onConnect: () => {
        this.connected = true;
        console.log('WebSocket connected');
      },
      onDisconnect: () => {
        this.connected = false;
      }
    });
    this.client.activate();
  }

  subscribe<T>(topic: string): Observable<T> {
    return new Observable<T>(observer => {
      const sub = this.client.subscribe(topic, (msg: IMessage) => {
        try {
          observer.next(JSON.parse(msg.body) as T);
        } catch {
          // ignore parse errors
        }
      });
      return () => sub.unsubscribe();
    });
  }

  disconnect(): void {
    if (this.client) this.client.deactivate();
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
