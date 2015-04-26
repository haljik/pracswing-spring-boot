package hello;

import org.jasig.cas.client.session.SingleSignOutFilter;
import org.jasig.cas.client.validation.Cas20ServiceTicketValidator;
import org.jasig.cas.client.validation.TicketValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.authentication.CasAssertionAuthenticationToken;
import org.springframework.security.cas.authentication.CasAuthenticationProvider;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.servlet.configuration.EnableWebMvcSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.ConcurrentSessionControlAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.session.ConcurrentSessionFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.AntPathRequestMatcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebMvcSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    CasAuthenticationFilter casFilter;

    @Autowired
    ConcurrentSessionFilter concurrentSessionFilter;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.addFilterBefore(requestLogoutFilter(), LogoutFilter.class);
        http.addFilterBefore(singleSignOutFilter(), ConcurrentSessionFilter.class);
        http.addFilter(concurrentSessionFilter);
        http.addFilter(casFilter);
        http.exceptionHandling().authenticationEntryPoint(casEntryPoint());
        http.authorizeRequests()
                .antMatchers("/", "/top", "/page2").permitAll()
                .anyRequest().authenticated()
                .and()
                .logout()
                .logoutSuccessUrl("/top")
                .permitAll();
    }


    //先勝ち設定↓
    //セッションイベントの発行をリスニング
    @Bean
    public static ServletListenerRegistrationBean httpSessionEventPublisher() {
        return new ServletListenerRegistrationBean(new HttpSessionEventPublisher());
    }

    // work around https://jira.spring.io/browse/SEC-2855
    // アプリケーションリスナーとして動き、セッション削除イベントに反応してRegistryから削除している。
    // 先勝ち設定の場合は、運用で該当ユーザのセッションを削除可能とする仕組みが必要となりそう。
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    // SessionAuthenticationStrategyのallowablesessionExceededで無効化したセッションが
    // ConcurrentSessionFilterで破棄される。
    // ここで先勝ち、後勝ちの制御ができる。
    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy(SessionRegistry registry) {
        ConcurrentSessionControlAuthenticationStrategy strategy = new ConcurrentSessionControlAuthenticationStrategy(registry) {
            @Override
            protected void allowableSessionsExceeded(List<SessionInformation> sessions, int allowableSessions, SessionRegistry registry) throws SessionAuthenticationException {
                SessionInformation expireTarget = sessions.stream()
                        .sorted((origin, other) -> -1 * origin.getLastRequest().compareTo(other.getLastRequest()))//-1をかけて反転させ先勝ち/なにもしないと後勝ち
                        .findFirst().get();
                expireTarget.expireNow();
            }
        };
        strategy.setMaximumSessions(1);
        return strategy;
    }

    @Bean
    public ConcurrentSessionFilter concurrentSessionFilter(SessionRegistry registry) {
        return new ConcurrentSessionFilter(registry, "/top?expired");//先勝ち、後勝ち両方で無効化されたセッションIDでアクセスした場合のリダイレクト先を指定
    }

    @Bean
    public CasAuthenticationFilter casFilter(SessionRegistry registry, SessionAuthenticationStrategy strategy) throws Exception {
        CasAuthenticationFilter filter = new CasAuthenticationFilter() {
            @Override
            public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException {
                final Authentication authentication = super.attemptAuthentication(request, response);
                if (authentication != null) {
                    //On Authentication Completed
                    registry.registerNewSession(request.getSession().getId(), authentication.getPrincipal());
                }
                return authentication;
            }
        };
        filter.setSessionAuthenticationStrategy(strategy);
        filter.setAuthenticationManager(authenticationManager());
        return filter;
    }
//後勝ち・先勝ち設定 ↑

    @Bean
    public LogoutFilter requestLogoutFilter() {
        SecurityContextLogoutHandler handler = new SecurityContextLogoutHandler();
        handler.setClearAuthentication(true);
        handler.setInvalidateHttpSession(true);
        final LogoutFilter logoutFilter = new LogoutFilter("https://localhost:9443/cas/logout", handler);
        logoutFilter.setLogoutRequestMatcher(new AntPathRequestMatcher("/logout"));
        return logoutFilter;
    }

    @Bean
    public SingleSignOutFilter singleSignOutFilter() {
        return new SingleSignOutFilter();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(casAuthenticationProvider());
    }

    @Bean
    public ServiceProperties serviceProperties() {
        ServiceProperties serviceProperties = new ServiceProperties();
        serviceProperties.setService("https://localhost:8443/j_spring_cas_security_check");
        serviceProperties.setSendRenew(false);
        return serviceProperties;
    }


    @Bean
    public CasAuthenticationEntryPoint casEntryPoint() {
        CasAuthenticationEntryPoint entryPoint = new CasAuthenticationEntryPoint();
        entryPoint.setLoginUrl("https://localhost:9443/login");
        entryPoint.setServiceProperties(serviceProperties());
        return entryPoint;
    }

    @Bean
    public CasAuthenticationProvider casAuthenticationProvider() {
        CasAuthenticationProvider casAuthenticationProvider = new CasAuthenticationProvider();
        casAuthenticationProvider.setAuthenticationUserDetailsService(authenticationUserDetailsService());
        casAuthenticationProvider.setServiceProperties(serviceProperties());
        casAuthenticationProvider.setTicketValidator(ticketValidator());
        casAuthenticationProvider.setKey("some_id_for_this_cas_prov");
        return casAuthenticationProvider;
    }

    @Bean
    public TicketValidator ticketValidator() {
        final Cas20ServiceTicketValidator cas20ServiceTicketValidator = new Cas20ServiceTicketValidator("https://localhost:9443");
        return cas20ServiceTicketValidator;
    }

    @Bean
    public AuthenticationUserDetailsService<CasAssertionAuthenticationToken> authenticationUserDetailsService() {
        return new UserDetailsByNameServiceWrapper<>(userDetailsServiceBean());
    }

    @Override
    public UserDetailsService userDetailsServiceBean() {
        return username -> new User(username, "",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
