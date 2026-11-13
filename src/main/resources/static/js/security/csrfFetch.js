(() => {
    const nativeFetch = window.fetch.bind(window);
    const tokenMeta = document.querySelector('meta[name="_csrf"]');
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');
    if (!tokenMeta || !headerMeta) return;

    const unsafeMethods = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);
    const excludedPaths = new Set(['/users/login', '/users/signup', '/users/refresh', '/users/logout']);
    const csrfToken = tokenMeta.content;
    const csrfHeader = headerMeta.content;

    function withCsrf(input, init) {
        const request = new Request(input, init);
        const headers = new Headers(request.headers);
        if (unsafeMethods.has(request.method.toUpperCase())) headers.set(csrfHeader, csrfToken);
        return new Request(request, { headers });
    }

    window.fetch = async (input, init) => {
        const request = withCsrf(input, init);
        let response = await nativeFetch(request.clone());
        const url = new URL(request.url, window.location.href);
        if (url.origin !== window.location.origin || response.status !== 401 || excludedPaths.has(url.pathname)) {
            return response;
        }

        const refreshRequest = withCsrf('/users/refresh', { method: 'POST', credentials: 'same-origin' });
        const refreshResponse = await nativeFetch(refreshRequest);
        if (!refreshResponse.ok) return response;
        response = await nativeFetch(request.clone());
        return response;
    };
})();
