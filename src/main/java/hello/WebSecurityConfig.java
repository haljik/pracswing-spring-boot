package hello;

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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import java.util.Collections;

@Configuration
@EnableWebMvcSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    CasAuthenticationFilter casFilter;

    @Autowired
    SessionRegistry registry;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.addFilter(casFilter);
        http.exceptionHandling().authenticationEntryPoint(casEntryPoint());
        http.authorizeRequests()
                .antMatchers("/", "/top", "/page2").permitAll()
                .anyRequest().authenticated()
                .and()
                .logout()
                .logoutSuccessUrl("/top")
                .permitAll();

        //SessionManagementFilterを有効にする。
        http.sessionManagement()
                .sessionAuthenticationErrorUrl("/top?allowableSessionsExceeded")
                .maximumSessions(1)
                .maxSessionsPreventsLogin(true) //falseで後勝ちとなる
                .expiredUrl("/top?sessionExpired") //セッション無効時のURL指定
                .sessionRegistry(registry);
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
    // cas側のtiketを無効化した段階でセッションが削除できれば良いのだが…
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public CasAuthenticationFilter casFilter(SessionRegistry registry) throws Exception {
        CasAuthenticationFilter filter = new CasAuthenticationFilter();
        //認証成功時にSessionRegistryにセッションを登録することでHttpSecurityのsessionManagement()で設定される
        //SessionManagementFilterでユーザごとのセッション数を検出可能とする。
        filter.setAuthenticationSuccessHandler(
                (request, response, authentication) ->
                        registry.registerNewSession(request.getSession().getId(), authentication.getPrincipal())
        );
        return filter;
    }
//先勝ち設定 ↑

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
