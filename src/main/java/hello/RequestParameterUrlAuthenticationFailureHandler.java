package hello;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * リクエストパラメータを使用した、ログイン失敗時に元の画面を表示するAuthenticationFailureHandler
 */
public class RequestParameterUrlAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    /**
     * パラメータが存在しない場合のデフォルト
     */
    private final String defaultLoginPage;

    /**
     * リクエストパラメータ名
     */
    private final String requestParameterName;

    /**
     * 失敗時に付け加えるクエリ
     */
    private final String failureQuery;

    /**
     * デフォルトURLを受け取るコンストラクタ
     *
     * @param defaultLoginPage デフォルトのログインページ
     * @param requestParameterName ログインページの場所を伝えるパラメータ名
     */
    public RequestParameterUrlAuthenticationFailureHandler(String defaultLoginPage, String requestParameterName) {
        // Spring Security のデフォルトと同様に error を付け加えるにしておく
        this(defaultLoginPage, requestParameterName, "error");
    }

    /**
     * デフォルトURLを受け取るコンストラクタ
     *
     * @param defaultLoginPage デフォルトのログインページ
     * @param requestParameterName ログインページの場所を伝えるパラメータ名
     * @param failureQuery 失敗時に付け加えるクエリ
     */
    public RequestParameterUrlAuthenticationFailureHandler(String defaultLoginPage, String requestParameterName, String failureQuery) {
        this.defaultLoginPage = defaultLoginPage;
        this.requestParameterName = requestParameterName;
        this.failureQuery = failureQuery;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        String failurePath = toFailurePath(getLoginPageOrDefault(request));

        // defaultFailureUrlにsetするとマルチスレッドで死にそうなので
        // SimpleUrlAuthenticationFailureHandlerの実装をコピー
        saveException(request, exception);
        if (isUseForward()) {
            logger.debug("Forwarding to " + failurePath);
            request.getRequestDispatcher(failurePath).forward(request, response);
        } else {
            logger.debug("Redirecting to " + failurePath);
            getRedirectStrategy().sendRedirect(request, response, failurePath);
        }
    }

    /**
     * ログインページのパスをパラメータから取る。
     * パラメータに無かったらデフォルト。
     *
     * @param request リクエスト
     * @return ログインページのパス
     */
    private String getLoginPageOrDefault(HttpServletRequest request) {
        String loginPage = request.getParameter(requestParameterName);
        if (loginPage == null || loginPage.isEmpty()) {
            return defaultLoginPage;
        }
        return loginPage;
    }

    /**
     * 失敗時のクエリを付け加えてfailurePathにする。
     *
     * @param loginPage ログインページ
     * @return failurePath 失敗時のパス
     */
    private String toFailurePath(String loginPage) {
        if (failureQuery.isEmpty()) return loginPage;

        // 他のクエリがあるかとか考慮する必要があるかどうか
        // Refererを使ったら出てくるかも
        return loginPage + "?" + failureQuery;
    }
}