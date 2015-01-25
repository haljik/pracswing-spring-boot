package hello;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * ログイン失敗時に元の画面を表示するためのダサい実装.
 *
 * {@link org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler} みたくしたいのだけど...
 */
public class FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        String from = request.getParameter("from");
        if (from == null || from.isEmpty()) {
            // パラメータに無ければloginにしておく。。。
            from = "login";
        }
        String failurePath = "/" + from + "?error";

        // defaultFailureUrlにsetするわけにはいかないので実装コピー。。。
        saveException(request, exception);
        if (isUseForward()) {
            logger.debug("Forwarding to " + failurePath);

            request.getRequestDispatcher(failurePath).forward(request, response);
        } else {
            logger.debug("Redirecting to " + failurePath);
            getRedirectStrategy().sendRedirect(request, response, failurePath);
        }
    }
}
