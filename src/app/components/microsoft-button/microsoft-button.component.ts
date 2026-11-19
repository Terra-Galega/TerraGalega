// LOGIN CON MICROSOFT DESACTIVADO (no se puede usar).
// La universidad no nos da permisos en Azure (Microsoft Entra ID) para registrar la
// aplicación y obtener el Client ID, así que no lo podemos usar. También se intentó
// con una cuenta personal de Microsoft, pero tampoco lo permitió.
// Se deja comentado por si en el futuro se consiguen los permisos.
//
// import { Component, EventEmitter, Input, OnInit, Output, signal } from '@angular/core';
// import { PublicClientApplication } from '@azure/msal-browser';
// import { MICROSOFT_CLIENT_ID } from '../../microsoft.config';
// import { TranslatePipe } from '../../services/translate.pipe';
//
// // Botón "Continuar con Microsoft". Imita al botón oficial de Google (píldora de 40 px, borde gris,
// // logo a la izquierda y texto centrado) para que los dos accesos se vean como una pareja.
// // Microsoft abre su ventana, el usuario elige su cuenta y se emite el ID token para el back.
// @Component({
  // selector: 'app-microsoft-button',
  // imports: [TranslatePipe],
  // template: `
    // @if (enabled) {
      // <button
        // type="button"
        // id="btn-microsoft"
        // class="ms-btn"
        // [disabled]="disabled || !ready() || busy()"
        // [attr.aria-busy]="busy()"
        // (click)="login()"
      // >
        // <!-- Logo oficial de Microsoft: cuatro cuadrados -->
        // <svg class="ms-logo" viewBox="0 0 21 21" aria-hidden="true" focusable="false">
          // <rect x="1" y="1" width="9" height="9" fill="#f25022" />
          // <rect x="11" y="1" width="9" height="9" fill="#7fba00" />
          // <rect x="1" y="11" width="9" height="9" fill="#00a4ef" />
          // <rect x="11" y="11" width="9" height="9" fill="#ffb900" />
        // </svg>
        // <span class="ms-text">{{ 'microsoft.continue' | t }}</span>
      // </button>
      // @if (error()) {
        // <p role="alert" class="text-sm text-center mt-2" style="color: #8a2222">{{ error() }}</p>
      // }
    // }
  // `,
  // styles: `
    // :host {
      // display: block;
      // width: 100%;
      // margin-bottom: 0.75rem;
    // }
//
    // /* Mismas medidas que el botón de Google (size: large, shape: pill, theme: outline) */
    // .ms-btn {
      // position: relative;
      // display: flex;
      // align-items: center;
      // justify-content: center;
      // width: 100%;
      // height: 40px;
      // padding: 0 12px;
      // border-radius: 9999px;
      // border: 1px solid #dadce0;
      // background: #fff;
      // color: #3c4043;
      // font-family: 'Segoe UI', Roboto, Arial, sans-serif;
      // font-size: 14px;
      // font-weight: 500;
      // letter-spacing: 0.25px;
      // cursor: pointer;
      // transition: background-color 0.2s, border-color 0.2s;
    // }
//
    // .ms-btn:hover:not(:disabled) {
      // background: #f8faff;
      // border-color: #d2e3fc;
    // }
//
    // .ms-btn:focus-visible {
      // outline: 2px solid #4285f4;
      // outline-offset: 2px;
    // }
//
    // .ms-btn:disabled {
      // cursor: default;
      // opacity: 0.6;
    // }
//
    // /* El logo va pegado a la izquierda, como la "G" en el de Google */
    // .ms-logo {
      // position: absolute;
      // left: 12px;
      // width: 18px;
      // height: 18px;
    // }
//
    // .ms-text {
      // padding: 0 28px;
      // white-space: nowrap;
      // overflow: hidden;
      // text-overflow: ellipsis;
    // }
//
    // /* Tema oscuro: igual que el botón "filled_black" de Google */
    // :host-context(html[data-theme='dark']) .ms-btn {
      // background: #131314;
      // border-color: #8e918f;
      // color: #e3e3e3;
    // }
//
    // :host-context(html[data-theme='dark']) .ms-btn:hover:not(:disabled) {
      // background: #1f1f20;
    // }
  // `,
// })
// export class MicrosoftButtonComponent implements OnInit {
  // @Input() disabled = false;
  // @Output() credential = new EventEmitter<string>();
  // // Vacío = el botón no aparece (igual que con Google)
  // readonly enabled = MICROSOFT_CLIENT_ID.trim().length > 0;
  // readonly ready = signal(false);
  // readonly busy = signal(false);
  // readonly error = signal('');
  // private client?: PublicClientApplication;
//
  // async ngOnInit() {
    // if (!this.enabled) return;
    // try {
      // this.client = new PublicClientApplication({
        // auth: {
          // clientId: MICROSOFT_CLIENT_ID,
          // authority: 'https://login.microsoftonline.com/common',
          // // Debe estar registrada en Entra como URI de redirección de tipo "SPA"
          // redirectUri: location.origin + '/login',
        // },
        // cache: { cacheLocation: 'memoryStorage' },
      // });
      // await this.client.initialize();
      // this.ready.set(true);
    // } catch {
      // this.error.set('No se pudo preparar el acceso con Microsoft.');
    // }
  // }
//
  // async login() {
    // if (!this.client || this.busy() || this.disabled) return;
    // this.error.set('');
    // this.busy.set(true);
    // try {
      // const account = await this.client.loginPopup({
        // scopes: ['openid', 'profile', 'email'],
        // prompt: 'select_account',
      // });
      // this.credential.emit(account.idToken);
    // } catch (error) {
      // // Cerrar la ventana de Microsoft no es un error para el usuario
      // const code = error instanceof Error && 'errorCode' in error ? error.errorCode : '';
      // if (code !== 'user_cancelled') {
        // this.error.set(
          // code === 'popup_window_error'
            // ? 'Tu navegador bloqueó la ventana de Microsoft. Permite las ventanas emergentes e inténtalo de nuevo.'
            // : 'No se pudo abrir o completar el acceso con Microsoft.',
        // );
      // }
    // } finally {
      // this.busy.set(false);
    // }
  // }
// }
